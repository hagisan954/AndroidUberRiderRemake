package jp.ac.hal.androiduberriderremake.Model.EventBus;

public class showNotificationFinishTrip {
    private String tripKey;

    public showNotificationFinishTrip(String tripKey) {
        this.tripKey = tripKey;
    }

    public String getTripKey() {
        return tripKey;
    }

    public void setTripKey(String tripKey) {
        this.tripKey = tripKey;
    }
}
