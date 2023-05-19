package framework.telegram.message.ui.location.bean;

import java.io.Serializable;

/**
 * Created by hyf on 15/9/11.
 */
public class POIBean implements Serializable {
    private static final long serialVersionUID = 3528898065509194281L;
    public String id;
    public String name;
    public String address;
    public long lat;
    public long lng;
    public boolean isCheck = false;

    public POIBean(){
    }

    public POIBean(String id,String name,String address,int lat,int lng){
        this.id = id;
        this.name = name;
        this.address = address;
        this.lat = lat;
        this.lng = lng;
        this.isCheck= false;

    }
}
