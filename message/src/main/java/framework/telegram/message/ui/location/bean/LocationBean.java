package framework.telegram.message.ui.location.bean;

import java.io.Serializable;

/**
 * Created by hu on 15/8/28.
 */
public class LocationBean implements Serializable {

    private static final long serialVersionUID = -1250585289942743094L;

    public double lat;
    public double lng;
    public String address;

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }
}
