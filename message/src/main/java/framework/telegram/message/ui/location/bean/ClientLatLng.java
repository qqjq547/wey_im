package framework.telegram.message.ui.location.bean;

import com.amap.api.maps.model.LatLng;

import java.io.Serializable;

/**
 * Created by hyf on 15/12/19.
 */
public class ClientLatLng implements Serializable {
    private static final long serialVersionUID = -1229384814237144239L;
    public double latitude;
    public double longitude;
    public long time;

    public ClientLatLng(){

    }
    public ClientLatLng(double latitude, double longitude, long time){
        this.latitude = latitude;
        this.longitude = longitude;
        this.time = time;
    }

    public LatLng getLatLng(){
        return new LatLng(latitude, longitude);
    }

//    public com.google.android.gms.maps.model.LatLng getLatLngGMap() {
//        return new com.google.android.gms.maps.model.LatLng(latitude, longitude);
//    }


    public long getLongLat(){
        return (long)(latitude*1000000);
    }
    public long getLongLng(){
        return (long)(longitude*1000000);
    }
}
