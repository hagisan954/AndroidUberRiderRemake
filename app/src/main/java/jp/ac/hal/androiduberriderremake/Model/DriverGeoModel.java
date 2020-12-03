package jp.ac.hal.androiduberriderremake.Model;

import com.firebase.geofire.GeoLocation;

public class DriverGeoModel {
    private String key;
    private GeoLocation geoLocation;
    private DriverInfoModel driverInfoModel;
    private boolean isDecline;

    public DriverGeoModel() {
    }

    public DriverGeoModel(String key, GeoLocation geoLocation) {
        this.key = key;
        this.geoLocation = geoLocation;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public GeoLocation getGeoLocation() {
        return geoLocation;
    }

    public void setGeoLocation(GeoLocation geoLocation) {
        this.geoLocation = geoLocation;
    }

    public DriverInfoModel getDriverInfoModel() {
        return driverInfoModel;
    }

    public void setDriverInfoModel(DriverInfoModel driverInfoModel) {
        this.driverInfoModel = driverInfoModel;
    }

    public boolean isDecline() {
        return isDecline;
    }

    public void setDecline(boolean decline) {
        isDecline = decline;
    }
}
