//package com.example.bread.fragment;
//
//import android.graphics.Bitmap;
//import android.graphics.Canvas;
//import android.graphics.Paint;
//import android.graphics.drawable.Drawable;
//import android.os.Bundle;
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.TextView;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.core.content.res.ResourcesCompat;
//import androidx.fragment.app.Fragment;
//
//import com.example.bread.R;
//import com.example.bread.model.MoodEvent;
//import com.example.bread.utils.EmotionUtils;
//import com.google.android.gms.maps.CameraUpdateFactory;
//import com.google.android.gms.maps.GoogleMap;
//import com.google.android.gms.maps.OnMapReadyCallback;
//import com.google.android.gms.maps.SupportMapFragment;
//import com.google.android.gms.maps.UiSettings;
//import com.google.android.gms.maps.model.BitmapDescriptorFactory;
//import com.google.android.gms.maps.model.LatLng;
//import com.google.android.gms.maps.model.MapStyleOptions;
//import com.google.android.gms.maps.model.MarkerOptions;
//import com.google.android.material.shape.MarkerEdgeTreatment;
//
//import org.w3c.dom.Text;
//
//import java.util.ArrayList;
//import java.util.Map;
//
//public class MapDisplayFragment extends Fragment implements OnMapReadyCallback {
////    MapFragment mapFragment;
//
//    @Nullable
//    @Override
//    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        View view = inflater.inflate(R.layout.fragment_map_display, container, false);
//
//        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.google_map);
//        if (mapFragment != null) {
//            mapFragment.getMapAsync(this);
//        } else {
//            Log.e("MapError", "SupportMapFragment is null");
//        }
//        return view;
//    }
//
////    //adding mood event icons to the map https://www.youtube.com/watch?v=4fExmOFKgQY
////    @Override
////    public void onMapReady(@NonNull GoogleMap googleMap) {
////        googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style));
////        LatLng uofa = new LatLng(53.5232, -113.5263);
////        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(uofa, 15));
////
////        // adding map UI controls https://www.youtube.com/watch?v=y84o2kyi_eo
////        // Get UI settings
////        UiSettings uiSettings = googleMap.getUiSettings();
////
////        // Enable map gestures and controls
////        uiSettings.setZoomControlsEnabled(true);
////        uiSettings.setZoomGesturesEnabled(true);
////        uiSettings.setCompassEnabled(true);
////        uiSettings.setRotateGesturesEnabled(true);
////        uiSettings.setTiltGesturesEnabled(true);
////        uiSettings.setScrollGesturesEnabled(true);
////        uiSettings.setScrollGesturesEnabledDuringRotateOrZoom(false);
////        uiSettings.setIndoorLevelPickerEnabled(true);
////
////        // Map markers
////        String user = "@user1".concat(": ");
////        MoodEvent.EmotionalState emotion = MoodEvent.EmotionalState.HAPPY;
////        String moodText = emotion.toString();
////        String emoji = EmotionUtils.getEmoticon(emotion);
////        String finalString = user.concat(moodText).concat(emoji);
////
////        MarkerOptions testMark = new MarkerOptions().position(uofa).title(finalString).icon(BitmapDescriptorFactory
//////                .fromBitmap(getBitmapFromDrawable(R.drawable.test_happy)));
////                .fromBitmap(textAsBitmap(emoji)));
////        googleMap.addMarker(testMark);
////    }
//
////    //https://www.youtube.com/watch?v=4fExmOFKgQY
////    private Bitmap getBitmapFromDrawable(int resId){
////        Bitmap bitmap = null;
////        Drawable drawable = ResourcesCompat.getDrawable(getResources(), resId, null);
////        if (drawable != null){
////            bitmap = Bitmap.createBitmap(50,50,Bitmap.Config.ARGB_8888);
////            Canvas canvas = new Canvas(bitmap);
////            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
////            drawable.draw(canvas);
////        }
////        return bitmap;
////    }
//
////    //https://stackoverflow.com/questions/8799290/convert-string-text-to-bitmap
////    public Bitmap textAsBitmap(String text) {
////        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
////        paint.setTextAlign(Paint.Align.LEFT);
////        paint.setTextSize(75);
////        float baseline = -paint.ascent(); // ascent() is negative
////        int width = (int) (paint.measureText(text) + 0.5f); // round
////        int height = (int) (baseline + paint.descent() + 0.5f);
////        Bitmap image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
////        Canvas canvas = new Canvas(image);
////        canvas.drawText(text, 0, baseline, paint);
////        return image;
////    }
//
////    public void displayHistoryOnMap(){
////        mapFragment.getMoodHistoryEvents(new MapFragment.MoodEventCallback() {
////            @Override
////            public void onResult(ArrayList<MoodEvent> moodEvents) {
////                for (MoodEvent event : moodEvents) {
////                    Log.d("MoodEvent", "Fetched event: " + event);
////                    MoodEvent.EmotionalState emotion = event.getEmotionalState();
////                    String moodText = emotion.toString();
////                    String emoji = EmotionUtils.getEmoticon(emotion);
////
////                    Map<String, Object> geo = event.getGeoInfo();
////                    if (geo != null){
////                        double lat = (double) geo.get("latitude");
////                        double lon = (double) geo.get("longitude");
////                    }
////                }
////            }
////        });
////
////        //variables
////
////        //loop through and extract information for each
////
////        //display on map
////
////    }
//}
