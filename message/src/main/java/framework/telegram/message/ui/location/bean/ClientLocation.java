package framework.telegram.message.ui.location.bean;

import java.io.Serializable;


/**
 * Created by hu on 15/3/16.
 * 从需求来看的话，这个类并没有用到
 */
public class ClientLocation implements Serializable {

    private static final long serialVersionUID = -5032036898854371364L;
    private int countryId;
    private int provinceId;
    private int cityId;
    private int districtId;

    private double latitude;
    private double longitude;
    private String addr;
    private long time;

    public ClientLocation() {

    }

    public void setCountryId(int countryId) {
        this.countryId = countryId;
    }

//    public CountryModel getCountry() {
//        return ProvinceController.getInstance().getCountryByCountryIdSync(AccountManager.getInstance().getLoginAccount(), countryId);
//    }

    public int getCountryId() {
        return countryId;
    }

    public boolean isChina() {
        return countryId == 1;
    }

//    public String getCountryName() {
//        CountryModel countryModel = getCountry();
//        if (countryModel != null) {
//            return countryModel.getNameByLanguage();
//        }
//
//        return "";
//    }
//
//    public ProvinceModel getProvince() {
//        return ProvinceCont  public CountryModel getCountry() {
//        return ProvinceController.getInstance().getCountryByCountryIdSync(AccountManager.getInstance().getLoginAccount(), countryId);
//    }roller.getInstance().getProvinceByIdSync(AccountManager.getInstance().getLoginAccount(), provinceId);
//    }

//    public String getProvinceName() {
//        ProvinceModel provinceModel = getProvince();
//        if (provinceModel != null) {
//            return provinceModel.getNameByLanguage();
//        }
//
//        return "";
//    }

    public int getProvinceId() {
        return provinceId;
    }

    public void setProvinceId(int provinceId) {
        this.provinceId = provinceId;
    }

//    public CityModel getCity() {
//        return ProvinceController.getInstance().getCityByIdSync(AccountManager.getInstance().getLoginAccount(), cityId);
//    }

//    public String getCityName() {
//        CityModel cityModel = getCity();
//        if (cityModel != null) {
//            return cityModel.getNameByLanguage();
//        }
//
//        return "";
//    }

    public void setCityId(int cityId) {
        this.cityId = cityId;
    }

    public int getCityId() {
        return cityId;
    }

//    public DistrictModel getDistrict() {
//        return ProvinceController.getInstance().getDistrictByIdSync(AccountManager.getInstance().getLoginAccount(), districtId);
//    }

//    public String getDistrictName() {
//        DistrictModel districtModel = getDistrict();
//        if (districtModel != null) {
//            return districtModel.getNameByLanguage();
//        }
//
//        return "";
//    }

    public void setDistrictId(int districtId) {
        this.districtId = districtId;
    }

    public int getDistrictId() {
        return districtId;
    }

//    @Override
//    public String toString() {
//        return getCountryName() + getProvinceName() + "," + getCityName() + "," + getDistrictName() + "," + getAddr();
//    }

    public ClientLatLng getClientLatLng() {
        return new ClientLatLng(latitude, longitude, time);
    }

    public long getLongLat() {
        return (long) (1000000.0f * latitude);
    }

    public long getLongLng() {
        return (long) (1000000.0f * longitude);
    }

    public double getFloatLat() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getFloatLng() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getAddr() {
        return addr == null ? "" : addr;
    }

    public void setAddr(String addr) {
        this.addr = addr;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }
}
