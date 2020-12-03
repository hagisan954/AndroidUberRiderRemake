package jp.ac.hal.androiduberriderremake.Model.EventBus;

public class DriveCompleteTripEvent {
    private String tripKey;

    public DriveCompleteTripEvent(String tripKey) {
        this.tripKey = tripKey;
    }

    public String getTripKey() {
        return tripKey;
    }

    public void setTripKey(String tripKey) {
        this.tripKey = tripKey;
    }
}
