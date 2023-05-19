package framework.telegram.message.ui.location.bean

/**
 * Created by lzh on 19-9-6.
 * INFO:
 */
data class GoogleLocationData(val results: ArrayList<GoogleLocationBean>,  var status: String,var next_page_token:String)

data class GoogleLocationBean(val geometry: GeometryLocation,  var id:String,var name: String,var vicinity:String)

data class GeometryLocation(val location: GoogleLocation)

data class GoogleLocation(val lat: Double,  var lng: Double)

