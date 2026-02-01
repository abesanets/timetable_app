package com.example.schedule.features.buses.data

import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.search.SearchFactory
import com.yandex.mapkit.search.SearchManager
import com.yandex.mapkit.search.SearchManagerType
import com.yandex.mapkit.search.Session
import com.yandex.mapkit.search.Response
import com.yandex.mapkit.transport.TransportFactory
import com.yandex.mapkit.transport.masstransit.TransitOptions
import com.yandex.mapkit.transport.masstransit.MasstransitRouter
import com.yandex.mapkit.transport.masstransit.Route
import com.yandex.mapkit.transport.masstransit.Section
import com.yandex.mapkit.transport.masstransit.Session.RouteListener
import com.yandex.mapkit.transport.masstransit.TimeOptions
import com.yandex.mapkit.transport.masstransit.Weight
import com.yandex.mapkit.transport.masstransit.FilterVehicleTypes
import com.yandex.mapkit.transport.masstransit.TravelEstimation
import com.yandex.runtime.Error
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import java.util.ArrayList

import com.yandex.mapkit.transport.masstransit.RouteStop

data class RouteResult(
    val id: String,
    val duration: String,
    val transfers: Int,
    val departureTime: String,
    val arrivalTime: String,
    val description: String
)

class RouteRepository {
    private val searchManager: SearchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED)
    private val router: MasstransitRouter = TransportFactory.getInstance().createMasstransitRouter()

    // Precise coordinates for MGKCT buildings
    private val KAZINTSA_POINT = Point(53.848383, 27.509325)
    private val KNORINA_POINT = Point(53.934112, 27.613628)

    suspend fun getRoutes(
        homeAddress: String,
        departureLocation: String,
        departureHour: Int,
        departureMinute: Int
    ): List<RouteResult> {
        val destinationPoint = findLocation(homeAddress) ?: throw Exception("Address not found")
        val originPoint = if (departureLocation.contains("Казинца")) KAZINTSA_POINT else KNORINA_POINT

        return fetchRoutes(originPoint, destinationPoint, departureHour, departureMinute)
    }

    private suspend fun findLocation(address: String): Point? = suspendCancellableCoroutine { continuation ->
        val query = if (address.contains("Минск", ignoreCase = true)) address else "Минск, $address"
        
        val geometry = com.yandex.mapkit.geometry.Geometry.fromBoundingBox(
            com.yandex.mapkit.geometry.BoundingBox(
                Point(53.83, 27.40),
                Point(53.97, 27.70)
            )
        )

        val searchSession = searchManager.submit(
            query,
            geometry,
            com.yandex.mapkit.search.SearchOptions(),
            object : Session.SearchListener {
                override fun onSearchResponse(response: Response) {
                    val point = response.collection.children.firstOrNull()?.obj?.geometry?.firstOrNull()?.point
                    if (continuation.isActive) {
                        continuation.resume(point)
                    }
                }

                override fun onSearchError(error: Error) {
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }
            }
        )
        
        continuation.invokeOnCancellation {
            searchSession.cancel()
        }
    }

    private suspend fun fetchRoutes(
        from: Point,
        to: Point,
        hour: Int,
        minute: Int
    ): List<RouteResult> = suspendCancellableCoroutine { continuation ->
        val requestedTime = getUnixTime(hour, minute)
        val currentTime = System.currentTimeMillis()
        
        // If requested time is more than 15 minutes in the past, use current time
        // to avoid "overnight" routes that look like 15-hour trips.
        val departureTime = if (requestedTime < currentTime - 15 * 60 * 1000) {
            currentTime
        } else {
            requestedTime
        }
        
        val timeOptions = TimeOptions()
        // If TimeOptions(null, departureTime) worked before, I should check why I changed it.
        // Actually, the previous code had TimeOptions(null, departureTime).
        // Let's try to find if it has a constructor with (Long?, Long?).
        // The error said "Date but Long? was expected" for the FIRST argument.
        // So the first argument is Long?.
        val timeOptionsFixed = TimeOptions(departureTime, null)
        
        val options = TransitOptions(
            FilterVehicleTypes.MINIBUS.value or FilterVehicleTypes.SUBURBAN.value,
            timeOptionsFixed
        )

        val requestPoints = listOf(
            com.yandex.mapkit.RequestPoint(from, com.yandex.mapkit.RequestPointType.WAYPOINT, null, null),
            com.yandex.mapkit.RequestPoint(to, com.yandex.mapkit.RequestPointType.WAYPOINT, null, null)
        )

        val session = router.requestRoutes(
            requestPoints,
            options,
            object : RouteListener {
                override fun onMasstransitRoutes(routes: MutableList<Route>) {
                    if (continuation.isActive) {
                        // Sort by duration first, then map to results, 
                        // and finally filter out minibuses and take top 5.
                        val results = routes
                            .sortedBy { it.metadata.weight.time.value }
                            .mapIndexed { index, route ->
                                mapRouteToResult(index.toString(), route, hour, minute)
                            }
                            .filter { result ->
                                val desc = result.description.lowercase()
                                !desc.contains("тк") && !desc.contains("так")
                            }
                            .take(5)
                        continuation.resume(results)
                    }
                }

                override fun onMasstransitRoutesError(error: Error) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(Exception("Error fetching routes: ${error}"))
                    }
                }
            }
        )
        
        continuation.invokeOnCancellation {
            session.cancel()
        }
    }
    
    private fun getUnixTime(hour: Int, minute: Int): Long {
        val calendar = java.util.Calendar.getInstance()
        // Ensure we are working with today's date but the requested hour/minute
        calendar.set(java.util.Calendar.HOUR_OF_DAY, hour)
        calendar.set(java.util.Calendar.MINUTE, minute)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        
        return calendar.timeInMillis
    }

    private fun mapRouteToResult(id: String, route: Route, startHour: Int, startMinute: Int): RouteResult {
        val weight = route.metadata.weight
        val duration = weight.time.text
        val transfers = weight.transfersCount
        
        val sections = route.sections
        val descriptionParts = ArrayList<String>()
        
        // Try to get absolute times from Yandex estimation
        var firstEstimationDeparture: Long? = null
        var lastEstimationArrival: Long? = null
        
        for (section in sections) {
            val estimation = section.metadata.getEstimation()
            if (estimation != null) {
                if (firstEstimationDeparture == null) {
                    firstEstimationDeparture = estimation.departureTime.value
                }
                lastEstimationArrival = estimation.arrivalTime.value
            }
        }
        
        val requestedDepartureMillis = getUnixTime(startHour, startMinute)
        val routeDepartureMillis = firstEstimationDeparture?.let { it * 1000 } ?: requestedDepartureMillis
        val routeArrivalMillis = lastEstimationArrival?.let { it * 1000 } ?: (routeDepartureMillis + (weight.time.value.toLong() * 1000))
        
        var currentAbsoluteTimeMillis = routeDepartureMillis
        
        for (i in sections.indices) {
            val section = sections[i]
            val data = section.metadata.data
            val estimation = section.metadata.getEstimation()
            
            // Use estimation time if available, otherwise fallback to accumulation
            val sectionStartTimeMillis = estimation?.departureTime?.value?.let { it * 1000 } ?: currentAbsoluteTimeMillis
            
            if (data.transports != null && data.transports!!.isNotEmpty()) {
                val transports = data.transports!!
                val distinctNames = transports.map { it.line.name }.distinct().take(3).joinToString(", ")
                
                var stopInfo = ""
                if (section.stops != null && section.stops.isNotEmpty()) {
                    val startStopName = section.stops.first().metadata.stop.name
                    val endStopName = section.stops.last().metadata.stop.name
                    
                    val stopCal = java.util.Calendar.getInstance().apply { timeInMillis = sectionStartTimeMillis }
                    val timePrefix = String.format("%02d:%02d, ", stopCal.get(java.util.Calendar.HOUR_OF_DAY), stopCal.get(java.util.Calendar.MINUTE))
                    
                    stopInfo = " (${timePrefix}ост. \"$startStopName\" ➝ ост. \"$endStopName\")"
                }
                
                descriptionParts.add("$distinctNames$stopInfo")
            } else {
                // Walking section
                var walkDesc = "Пешком"
                
                if (i + 1 < sections.size) {
                    val nextSection = sections[i+1]
                    val nextData = nextSection.metadata.data
                    if (nextData.transports != null && nextData.transports!!.isNotEmpty()) {
                        val nextStopName = nextSection.stops?.firstOrNull()?.metadata?.stop?.name
                        if (nextStopName != null) {
                            walkDesc = "Пешком до ост. \"$nextStopName\""
                        } else {
                            walkDesc = "Пешком к транспорту"
                        }
                    }
                } else {
                    walkDesc = "Пешком до дома"
                }
                
                if (descriptionParts.isNotEmpty() && descriptionParts.last() == "Пешком" && walkDesc != "Пешком") {
                    descriptionParts[descriptionParts.lastIndex] = walkDesc
                } else if (descriptionParts.isEmpty() || descriptionParts.last() != walkDesc) {
                    descriptionParts.add(walkDesc)
                }
            }
            
            // Update time for next section
            currentAbsoluteTimeMillis = estimation?.arrivalTime?.value?.let { it * 1000 } 
                ?: (sectionStartTimeMillis + section.metadata.weight.time.value.toLong() * 1000)
        }
        
        val description = descriptionParts.joinToString("\n↓\n")
        
        val departureCalendar = java.util.Calendar.getInstance().apply { timeInMillis = routeDepartureMillis }
        val arrivalCalendar = java.util.Calendar.getInstance().apply { timeInMillis = routeArrivalMillis }
        
        return RouteResult(
            id = id,
            duration = duration,
            transfers = transfers,
            departureTime = String.format("%02d:%02d", 
                departureCalendar.get(java.util.Calendar.HOUR_OF_DAY), 
                departureCalendar.get(java.util.Calendar.MINUTE)),
            arrivalTime = String.format("%02d:%02d", 
                arrivalCalendar.get(java.util.Calendar.HOUR_OF_DAY), 
                arrivalCalendar.get(java.util.Calendar.MINUTE)),
            description = description
        )
    }
}