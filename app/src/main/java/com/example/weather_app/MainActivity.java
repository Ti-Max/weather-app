package com.example.weather_app;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    String weatherApiKey = Env.weatherApiKey;

    final String CHANNEL_ID = "channel_weather";
    public static final int NOTIFICATION_ID = 888;

    RequestQueue queue;

    TextView errorView;

    // current weather
    TextView tempView;
    TextView cityNameView;
    TextView regionNameView;
    TextView feelsLikeView;
    TextView conditionView;
    ImageView conditionImage;

    // Forecast for today
    LinearLayout todayForecastLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        createNotificationChannel();
        setViews();

        // Notifications
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.location)
                .setContentTitle("title")
                .setContentText("content")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        //show the notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        // send it
        notificationManager.notify(NOTIFICATION_ID, builder.build());

        //TODO: Get location from IP address
        sendWeatherRequest("Stavanger");
    }
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "forecast";
            String description = "weather";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
    private void setViews(){
        queue = Volley.newRequestQueue(this);
        tempView = (TextView) findViewById(R.id.current);
        cityNameView = (TextView) findViewById(R.id.cityName);
        regionNameView = (TextView) findViewById(R.id.regionName);
        conditionView= (TextView) findViewById(R.id.condition);
        feelsLikeView = (TextView) findViewById(R.id.feelsLike);
        conditionImage = (ImageView) findViewById(R.id.conditionImage);
        errorView = (TextView) findViewById(R.id.error);
        todayForecastLayout = (LinearLayout) findViewById(R.id.todayForecastLayout);

        SearchView simpleSearchView = (SearchView) findViewById(R.id.search);

        simpleSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                sendWeatherRequest(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
    }

    private void updateDayForecast(JSONObject response) throws JSONException {
        JSONArray hours = null;
        try {
            hours = response.getJSONObject("forecast").getJSONArray("forecastday").getJSONObject(0).getJSONArray("hour");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        todayForecastLayout.removeAllViews();

        for (int i = 0; i < hours.length(); i++) {
            if (hours.getJSONObject(i).getInt("time_epoch") > (System.currentTimeMillis()/1000)){
                View view = LayoutInflater.from(this).inflate(R.layout.forecast_hour, null);

                //Set time
                View time = findChildById(((ViewGroup) view), R.id.hourTime);
                if (time != null){
                    ((TextView)time).setText(hours.getJSONObject(i).getString("time").substring(11));
                }

                //Set temp
                View temp = findChildById(((ViewGroup) view), R.id.tempViewDay);
                if (temp != null){
                    ((TextView)temp).setText(formatTempString(hours.getJSONObject(i).getString("temp_c"))+ "°");
                }
                ImageView icon = (ImageView) findChildById(((ViewGroup) view), R.id.conditionIcon);
                if (icon != null){
                    Picasso.get().load("https:"+hours.getJSONObject(i).getJSONObject("condition").getString("icon")).into(icon);
                }


                todayForecastLayout.addView(view);
            }

        }
    }

    private View findChildById(ViewGroup group, int id){
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child.getId() == id){
                return child;
            }else{
                if (child instanceof ViewGroup){

                    View child2 = findChildById(((ViewGroup) child),id);
                    if(child2 !=null){
                        return child2;
                    }
                }
            }
        }
        return null;
    }
    private void updateCurrent(JSONObject response){
        try {
            tempView.setText(formatTempString(response.getJSONObject("current").getString("temp_c")) + "°");
            cityNameView.setText(response.getJSONObject("location").getString("name"));
            regionNameView.setText(response.getJSONObject("location").getString("region"));
            feelsLikeView.setText("Feels like " + formatTempString(response.getJSONObject("current").getString("feelslike_c")) + "°");
            conditionView.setText(response.getJSONObject("current").getJSONObject("condition").getString("text"));

            Picasso.get().load("https:"+response.getJSONObject("current").getJSONObject("condition").getString("icon")).into(conditionImage);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void sendWeatherRequest(String place){
        String url = "https://api.weatherapi.com/v1/forecast.json?key="+weatherApiKey+"&q=" + place + "&days=1&aqi=no&alerts=no";
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(1, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response)
                    {
                        errorView.setText("");
                        updateCurrent(response);
                        try {
                            updateDayForecast(response);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error)
                    {
                        errorView.setText("\"" + place+ "\" was not found!");
                    }
                }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String>  params = new HashMap<String, String>();
                params.put("Content-Type", "application/json");

                return params;
            }
        };
        queue.add(jsonObjectRequest);
    }

    private String formatTempString(String temp){
        String format = "";
        for (int i = 0; i < temp.length(); i++){
            if (temp.charAt(i) == '.')
                break;
            else
                format += temp.charAt(i);
        }
        return format;

    }
}