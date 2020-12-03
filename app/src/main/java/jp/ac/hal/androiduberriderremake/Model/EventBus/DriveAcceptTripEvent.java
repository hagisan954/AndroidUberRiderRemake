package jp.ac.hal.androiduberriderremake.Model.EventBus;

public class DriveAcceptTripEvent {
    private String tripIp;

    public DriveAcceptTripEvent(String tripIp) {
        this.tripIp = tripIp;
    }

    public String getTripIp() {
        return tripIp;
    }

    public void setTripIp(String tripIp) {
        this.tripIp = tripIp;
    }
}
