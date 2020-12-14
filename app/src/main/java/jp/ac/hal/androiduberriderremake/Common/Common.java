package jp.ac.hal.androiduberriderremake.Common;

import android.animation.ValueAnimator;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.core.app.NotificationCompat;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.maps.android.ui.IconGenerator;

import java.net.CookieHandler;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jp.ac.hal.androiduberriderremake.Model.AnimationModel;
import jp.ac.hal.androiduberriderremake.Model.DriverGeoModel;
import jp.ac.hal.androiduberriderremake.Model.RiderModel;
import jp.ac.hal.androiduberriderremake.R;
import jp.ac.hal.androiduberriderremake.RequestDriverActivity;

public class Common {


    public static final String Rider_INFO_REFERENCE = "Riders";
    public static final String TOKEN_REFERENCE = "Token";
    public static final String DRIVERS_LOCATION_REFERENCES = "DriversLocation"; //Same as Driver app
    public static final String DRIVER_INFO_REFERENCE = "DriverInfo";
    public static final String REQUEST_DRIVER_TITLE = "RequestDriver";
    public static final String RIDER_PICKUP_LOCATION = "PickupLocation";
    public static final String RIDER_KEY = "RiderKey";
    public static final String REQUEST_DRIVER_DECLINE = "Decline";
    public static final String RIDER_PICKUP_LOCATION_STRING = "PickupLocationString";
    public static final String RIDER_DESTINATION_STRING = "DestinationLocationString";
    public static final String RIDER_DESTINATION = "DestinationLocation";
    public static final String REQUEST_DRIVER_ACCEPT = "Accept";
    public static final String TRIP_KEY = "TripKey";
    public static final String TRIP = "Trips"; //Same as name reference in Firebase
    public static final String REQUEST_DRIVER_DECLINE_AND_REMOVE_TRIP = "DeclineAndRemoveTrip";
    public static final String RIDER_COMPLETE_TRIP = "DriverCompleteTrip";

    public static RiderModel currentRider;

    public static final String NOTI_TITLE = "title";
    public static final String NOTI_CONTENT = "body";
    public static Map<String,DriverGeoModel> driversFound = new HashMap<>();
    public static HashMap<String, Marker> markerList = new HashMap<>();
    public static HashMap<String, AnimationModel> driverLocationSubscribe = new HashMap<String, AnimationModel>();

    public static String buildWelcomeMessage() {
        if(Common.currentRider != null)
        {
            return new StringBuilder("ようこそ")
                    .append(Common.currentRider.getFirstname())
                    .append(" ")
                    .append(Common.currentRider.getLastname()).toString();
        }
        else
        {
            return "";
        }

    }

    public static void showNotification(Context context, int id, String title, String body, Intent intent) {
        PendingIntent pendingIntent = null;
        if(intent != null)
            pendingIntent = PendingIntent.getActivity(context,id,intent,PendingIntent.FLAG_UPDATE_CURRENT);
        String NOTIFICATION_CHANNEL_ID = "edmt_dev_uber_remake";
        NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                    "Uber Remake",NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.setDescription("Uber Remake");
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setVibrationPattern(new long[]{0,1000,500,1000});
            notificationChannel.enableVibration(true);

            notificationManager.createNotificationChannel(notificationChannel);
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context,NOTIFICATION_CHANNEL_ID);
        builder.setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(false)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_VIBRATE)
                .setSmallIcon(R.drawable.ic_directions_car_black_24dp)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(),R.drawable.ic_directions_car_black_24dp));
        if(pendingIntent != null)
        {
            builder.setContentIntent(pendingIntent);
        }
        Notification notification = builder.build();
        notificationManager.notify(id,notification);
    }

    public static String buildName(String firstname, String lastname) {
        return new StringBuilder(firstname).append(" ").append(lastname).toString();
    }

    public static List<LatLng> decodePoly(String encoded) {
        List poly = new ArrayList();
        int index=0,len=encoded.length();
        int lat=0,lng=0;
        while(index < len)
        {
            int b,shift=0,result=0;
            do{
                b=encoded.charAt(index++)-63;
                result |= (b & 0x1f) << shift;
                shift+=5;

            }while(b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1):(result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do{
                b = encoded.charAt(index++)-63;
                result |= (b & 0x1f) << shift;
                shift +=5;
            }while(b >= 0x20);
            int dlng = ((result & 1)!=0 ? ~(result >> 1): (result >> 1));
            lng +=dlng;

            LatLng p = new LatLng((((double)lat / 1E5)),
                    (((double)lng/1E5)));
            poly.add(p);
        }
        return poly;
    }

    public static float getBearing(LatLng begin, LatLng end) {
        //You can copy this function by link at description
        double lat = Math.abs(begin.latitude - end.latitude);
        double lng = Math.abs(begin.longitude - end.longitude);

        if (begin.latitude < end.latitude && begin.longitude < end.longitude)
            return (float) (Math.toDegrees(Math.atan(lng / lat)));
        else if (begin.latitude >= end.latitude && begin.longitude < end.longitude)
            return (float) ((90 - Math.toDegrees(Math.atan(lng / lat))) + 90);
        else if (begin.latitude >= end.latitude && begin.longitude >= end.longitude)
            return (float) (Math.toDegrees(Math.atan(lng / lat)) + 180);
        else if (begin.latitude < end.latitude && begin.longitude >= end.longitude)
            return (float) ((90 - Math.toDegrees(Math.atan(lng / lat))) + 270);
        return -1;
    }

    public static void setWelcomeMessage(TextView txt_welcome) {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if(hour >= 4 && hour <= 12)
            txt_welcome.setText(new StringBuilder("おはようございます。"));
        else if(hour >= 13 && hour <= 17)
            txt_welcome.setText(new StringBuilder("こんにちは。"));
        else
            txt_welcome.setText(new StringBuilder("こんばんは。"));
    }

    public static String formatDuration(String duration) {
        if(duration.contains("mins"))
            return duration.substring(0,duration.length()-1); //Remove letter "s"
        else
            return duration;
    }

    public static String formatAddress(String start_address) {
        int firstIndexOfComma = start_address.indexOf(",");
        return start_address.substring(0,firstIndexOfComma); //Get only address
    }

    public static ValueAnimator valueAnimate(long duration,ValueAnimator.AnimatorUpdateListener listener) {
        ValueAnimator va = ValueAnimator.ofFloat(0,100);
        va.setDuration(duration);
        va.addUpdateListener(listener);
        va.setRepeatCount(ValueAnimator.INFINITE);
        va.setRepeatMode(ValueAnimator.RESTART);

        va.start();
        return va;
    }

    public static Bitmap createIconWithDuration(Context context, String duration) {
        View view = LayoutInflater.from(context).inflate(R.layout.pickup_info_with_duration_windows,null);
        TextView txt_time = (TextView)view.findViewById(R.id.txt_duration);
        txt_time.setText(Common.getNumberFromText(duration));

        IconGenerator generator = new IconGenerator(context);
        generator.setContentView(view);
        generator.setBackground(new ColorDrawable(Color.TRANSPARENT));
        return generator.makeIcon();
    }

    private static String getNumberFromText(String duration) {
        return  duration.substring(0,duration.indexOf(" "));
    }
}
