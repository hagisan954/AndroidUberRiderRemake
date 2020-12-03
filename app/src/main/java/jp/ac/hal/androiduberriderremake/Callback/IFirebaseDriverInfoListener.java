package jp.ac.hal.androiduberriderremake.Callback;

import jp.ac.hal.androiduberriderremake.Model.DriverGeoModel;

public interface IFirebaseDriverInfoListener {
    void onDriverInfoLoadSuccess(DriverGeoModel driverGeoModel);
}
