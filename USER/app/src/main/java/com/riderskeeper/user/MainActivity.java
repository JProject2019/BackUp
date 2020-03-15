package com.riderskeeper.user;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import net.daum.mf.map.api.MapPOIItem;
import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import io.realm.Realm;
import io.realm.RealmResults;
import java.util.Timer;
import java.util.TimerTask;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.LinkedList;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;

public class MainActivity extends AppCompatActivity{

    double latitude;
    double longitude;
    String RKlatitude;
    String RKlongitude;
    String[] array;
    boolean isRunningForeground = true;         //APP running status
    boolean isLocked = false;                   //RidersKeeper running status
    boolean workingStat = true;
    MapView mapView;
    MapPOIItem userLocation = new MapPOIItem(); //user location marker
    MapPOIItem bikeLocation = new MapPOIItem(); //bike location marker
    String imageViewID;                         //imageView id
    Realm realm;                                //local database

    Handler msghandler;
    SocketClient client;
    ReceiveThread receive;
    Socket socket;
    LinkedList<SocketClient> threadList;
    String key;
    String serverIP = "172.30.1.30";
    String serverPort = "8080";


//onCreate
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//connect to server
        threadList = new LinkedList<MainActivity.SocketClient>();

        TelephonyManager mgr = (TelephonyManager) getApplication().getSystemService(Context.TELEPHONY_SERVICE);
        key = mgr.getDeviceId();

        client = new SocketClient(serverIP, serverPort);
        threadList.add(client);
        client.start();

//local database
        Realm.init(this);
        realm = Realm.getDefaultInstance();

//initialize UI
        initUI();

//Create MapView
        mapView = new MapView(this);
        ViewGroup mapViewContainer = findViewById(R.id.map_view);
        mapViewContainer.addView(mapView);

//handler
        msghandler = new Handler() {
            @Override
            public void handleMessage(Message hdmsg) {
                if (hdmsg.what == 1111) {
                    //DB add
                    realm.beginTransaction();
                    localDB localdb = realm.createObject(localDB.class);

                    localdb.setID(hdmsg.obj.toString());
                    localdb.setStatus("OFF");
                    localdb.setImageURI(null);

                    realm.commitTransaction();

                    //UI add
                    addBicycle(hdmsg.obj.toString());

                    //toast message
                    Toast toast = Toast.makeText(MainActivity.this, "\"" + hdmsg.obj.toString() + "\" added", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL, 0, 0);
                    toast.show();
                }
                else if (hdmsg.what == 2222) {
                    //toast message
                    Toast toast = Toast.makeText(MainActivity.this, "\"" + hdmsg.obj.toString() + "\" is not a valid ID", Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL, 0, 0);
                    toast.show();
                }
                else if (hdmsg.what == 3333){
                    mapView.removePOIItem(bikeLocation);
                    bikeLocation.setMapPoint(MapPoint.mapPointWithGeoCoord(Double.parseDouble(RKlatitude), Double.parseDouble(RKlongitude)));
                    mapView.addPOIItem(bikeLocation);
                    mapView.setMapCenterPoint(MapPoint.mapPointWithGeoCoord(Double.parseDouble(RKlatitude), Double.parseDouble(RKlongitude)), true);
                }
                else if (hdmsg.what == 4444){
                    workingStat = false;
                    //alarm dialog
                    mapView.removePOIItem(bikeLocation);
                    bikeLocation.setMapPoint(MapPoint.mapPointWithGeoCoord(Double.parseDouble(RKlatitude), Double.parseDouble(RKlongitude)));
                    mapView.addPOIItem(bikeLocation);
                    mapView.setMapCenterPoint(MapPoint.mapPointWithGeoCoord(Double.parseDouble(RKlatitude), Double.parseDouble(RKlongitude)), true);

                    new SendThread(socket, "alarm").start();
                    new SendThread(socket, "ABC123").start();
                    SetAlertDialog();
                }
                else {}
            }
        };

//set userLocation marker
        userLocation.setItemName("You are Here");
        userLocation.setTag(0);
        userLocation.setMarkerType(MapPOIItem.MarkerType.CustomImage); // marker design
        userLocation.setSelectedMarkerType(MapPOIItem.MarkerType.CustomImage);
        userLocation.setCustomImageResourceId(R.mipmap.usermarker);

//set bike Location marker
        bikeLocation.setItemName("Your Bike");
        bikeLocation.setTag(1);
        bikeLocation.setMarkerType(MapPOIItem.MarkerType.CustomImage); // marker design
        bikeLocation.setSelectedMarkerType(MapPOIItem.MarkerType.CustomImage);
        bikeLocation.setCustomImageResourceId(R.mipmap.rkmarker);

//user's location
        LocationManager locationManager = (LocationManager) getApplication().getSystemService(Context.LOCATION_SERVICE);
        Location location = locationManager.getLastKnownLocation(locationManager.NETWORK_PROVIDER);
        if (location == null){
            location = locationManager.getLastKnownLocation(locationManager.GPS_PROVIDER);
        }
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        mapView.setMapCenterPoint(MapPoint.mapPointWithGeoCoord(latitude, longitude), true);

//user location update
        Timer gps_timer = new Timer();
        TimerTask gps_TT = new TimerTask() {
            @Override
            public void run() {
                MainActivity.this.runOnUiThread(new Runnable(){
                    public void run(){
                        if (isRunningForeground && workingStat == true) {
                            //get user location
                            LocationManager locationManager = (LocationManager) getApplication().getSystemService(Context.LOCATION_SERVICE);
                            Location location = locationManager.getLastKnownLocation(locationManager.NETWORK_PROVIDER);
                            if (location == null){
                                location = locationManager.getLastKnownLocation(locationManager.GPS_PROVIDER);
                            }
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();

                            //update user location on map
                            mapView.removePOIItem(userLocation);
                            userLocation.setMapPoint(MapPoint.mapPointWithGeoCoord(latitude, longitude));
                            mapView.addPOIItem(userLocation);

                            //other RKs location update
                            if (!realm.isEmpty() ) new SendThread(socket, "gps").start();
                        }
                    }
                });
            }
        };
        gps_timer.schedule(gps_TT, 0, 2000);

//gps button - toasts current location
        Button gpsButton = findViewById(R.id.clickGPS);
        gpsButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mapView.setMapCenterPoint(MapPoint.mapPointWithGeoCoord(latitude, longitude), true);
                Toast toast = Toast.makeText(MainActivity.this, "User Latitude:" + latitude + "\nUser Longitude:" + longitude +
                        "\n\nBike Latitude:" + RKlatitude + "\nBike Longitude:" + RKlongitude, Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER_HORIZONTAL|Gravity.CENTER_VERTICAL, 0, 0);
                toast.show();

                //user location
                mapView.setMapCenterPoint(MapPoint.mapPointWithGeoCoord(latitude, longitude), true);
            }
        });

//edit button - add/delete bicycles
        Button editButton = findViewById(R.id.edit);
        editButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v){
                Intent intent = new Intent(MainActivity.this, edit.class);
                startActivityForResult(intent, 0);
            }
        });
    }
//End of onCreate



//onActivityResult
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        //imageVIew
        if(requestCode == 1 && resultCode == RESULT_OK){
            Uri selectedImageUri = data.getData();

            RealmResults<localDB> results = realm.where(localDB.class).equalTo("ID", imageViewID).findAll();
            for(localDB RK : results){
                realm.beginTransaction();
                RK.setImageURI(selectedImageUri.toString());
                realm.commitTransaction();
            }

            LinearLayout rootLayout = findViewById(R.id.root);
            LinearLayout ll = rootLayout.findViewWithTag("ll"+ imageViewID);
            ImageView imageView = ll.findViewWithTag("image"+ imageViewID);
            imageView.setImageURI(selectedImageUri);
        }

        switch (resultCode) {
            //do nothing
            case 0:
                break;

            //edit > addButton
            case 1:
                //send to Server, check if exists
                new SendThread(socket, "checkID").start();
                new SendThread(socket,data.getStringExtra("id")).start();
                break;

            //edit > deleteButton
            case 2:
                //DB delete
                RealmResults<localDB> results = realm.where(localDB.class).equalTo("ID", data.getStringExtra("id")).findAll();
                realm.beginTransaction();
                results.deleteAllFromRealm();
                realm.commitTransaction();

                //UI delete
                deleteBicycle(data.getStringExtra("id"));
                break;

            //edit > on/off
            case 3:
                //UI update
                int c = data.getIntExtra("color",0);
                setConnectionColor(data.getStringExtra("id"), c);

                //DB update
                RealmResults<localDB> results2 = realm.where(localDB.class).equalTo("ID", data.getStringExtra("id")).findAll();
                realm.beginTransaction();
                for(localDB localdb2 : results2){
                    if (c == 1) localdb2.setStatus("ON");
                    else if (c == 0) localdb2.setStatus("OFF");
                    else localdb2.setStatus("ERROR");
                }
                realm.commitTransaction();
                break;

            //reset after camera Activity
            case 4:
                new SendThread(socket, "location").start();
                new SendThread(socket, "ABC123").start();

                workingStat = true;
                break;

            //default
            default:
                break;
        }
    }

//add new bicycles
    public void addBicycle(String id){

        LinearLayout rootLayout = findViewById(R.id.root);

        //LinearLayout
        LinearLayout ll = new LinearLayout(this);
        ll.setTag("ll" + id);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(410, LinearLayout.LayoutParams.MATCH_PARENT);
        ll.setLayoutParams(layoutParams);

        //Camera Button
        Button cameraButton = new Button(this);
        cameraButton.setTag("camera"+id);
        cameraButton.setBackground(ContextCompat.getDrawable(this, R.mipmap.camera));
        ll.addView(cameraButton);
        ViewGroup.MarginLayoutParams camera_params = (ViewGroup.MarginLayoutParams) cameraButton.getLayoutParams();
        camera_params.width = 100; camera_params.height = 100;
        camera_params.leftMargin = 90; camera_params.topMargin = 15; camera_params.bottomMargin = 10;
        cameraButton.setLayoutParams(camera_params);

        //Image Button
        ImageView imageView = new ImageView(this);
        imageView.setTag("image"+id);

        RealmResults<localDB> results = realm.where(localDB.class).equalTo("ID", id).findAll();
        for(localDB RK : results){
            if (RK.getImageURI() == null)imageView.setImageResource(R.drawable.ic_launcher_foreground);
            else imageView.setImageURI(Uri.parse(RK.getImageURI()));
        }

        GradientDrawable drawable= (GradientDrawable) this.getDrawable(R.drawable.background_rounding);
        imageView.setBackground(drawable);
        imageView.setClipToOutline(true);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

        ll.addView(imageView);
        imageView.getLayoutParams().width = 275;
        imageView.getLayoutParams().height = 275;
        imageView.requestLayout();

        //LinearLayout - id, connection
        LinearLayout llayout = new LinearLayout(this);
        llayout.setTag("llayout"+id);
        llayout.setOrientation(LinearLayout.HORIZONTAL);
        llayout.setGravity(Gravity.CENTER);

        //TextView - id
        TextView id_text = new TextView(this);
        id_text.setTag("ID"+id);
        id_text.setTextColor(Color.parseColor("#FFFFFF"));
        id_text.setText(id);

        //TextView - connection
        TextView connection_color = new TextView(this);
        connection_color.setTag("color"+id);
        connection_color.setTextColor(Color.parseColor("#FF4040"));
        connection_color.setText(" ●");

        //addView
        llayout.addView(id_text);
        llayout.addView(connection_color);
        ll.addView(llayout);
        rootLayout.addView(ll);

        //camera button onClickListener
        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                workingStat = false;

                new SendThread(socket, "camera").start();
                new SendThread(socket, "ABC123").start();
                Intent camera_intent = new Intent(MainActivity.this, camera.class);
                startActivityForResult(camera_intent, 0);
            }
        });

        //image button onClickListener
        imageView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String s = v.getTag().toString();
                String s2 = s.replace("image", "");
                imageViewID = s2;

                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setDataAndType(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
                startActivityForResult(intent, 1);
            }
        });
    }

//delete selected bicycle
    public void deleteBicycle(String id){
        LinearLayout rootLayout = findViewById(R.id.root);
        LinearLayout ll = rootLayout.findViewWithTag("ll"+id);
        rootLayout.removeView(ll);

        mapView.removePOIItem(bikeLocation);
        isLocked = false;
    }

//change connection color
    public void setConnectionColor(String id, int c){
        LinearLayout rootLayout = findViewById(R.id.root);
        LinearLayout ll = rootLayout.findViewWithTag("ll"+id);
        LinearLayout llayout = ll.findViewWithTag("llayout"+id);
        TextView colorText = llayout.findViewWithTag("color"+id);

        //ON: 1, OFF: 0, ERROR: -1
        if (c != 1) {
            colorText.setTextColor(Color.parseColor("#FF4040")); //red
            isLocked = false;
        }
        else {
            colorText.setTextColor(Color.parseColor("#00AC00")); //green
            isLocked = true;
        }
    }

//initialize UI
    public void initUI(){
        RealmResults<localDB> results = realm.where(localDB.class).findAll();
        for(localDB RK : results){
            addBicycle(RK.getID());

            realm.beginTransaction();
            RK.setStatus("OFF");
            realm.commitTransaction();
        }
    }

//alarm dialog
private void SetAlertDialog(){
    AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
    alert.setPositiveButton("STOP", new DialogInterface.OnClickListener() {
        @Override public void onClick(DialogInterface dialog, int which) {
            Toast.makeText(MainActivity.this, "Alarm OFF", Toast.LENGTH_SHORT).show();
            new SendThread(socket, "alarmOFF").start();
            new SendThread(socket, "ABC123").start();
            dialog.dismiss();

            workingStat = true;
        }
    });
    alert.setMessage("\nABC123\n ALARM");
    alert.show();
}

//onStop
    @Override
    protected  void onStop(){
        isRunningForeground = false;
        super.onStop();
    }

//onResume
    @Override
    protected  void onResume(){
        isRunningForeground = true;
        super.onResume();
    }

//onDestroy
    @Override
    protected void onDestroy() {
        realm.close();
        super.onDestroy();
    }

//SocketClient
    class SocketClient extends Thread {
        boolean threadAlive;
        String ip;
        String port;

        private DataOutputStream output = null;

        private SocketClient(String ip, String port) {
            threadAlive = true;
            this.ip = ip;
            this.port = port;
        }

        @Override
        public void run() {
            try {
                socket = new Socket(ip, Integer.parseInt(port));

                output = new DataOutputStream(socket.getOutputStream());
                receive = new ReceiveThread(socket);
                receive.start();
                output.writeUTF(key);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

//ReceiveThread
    class ReceiveThread extends Thread{
        private Socket socket = null;
        DataInputStream input;

        public ReceiveThread(Socket socket){
            this.socket = socket;
            try{
                input = new DataInputStream(socket.getInputStream());
            }catch(Exception e) {
            }
        }

        public void run(){
            try{
                while(input != null){
                    String msg = input.readUTF();

                //get id check result
                    if (msg.equals("idResult")){

                        String idRes = input.readUTF();

                        if (idRes.equals("valid")){ //valid id
                            String id = input.readUTF();
                            Message hdmsg = msghandler.obtainMessage();
                            hdmsg.what = 1111;
                            hdmsg.obj = id;
                            msghandler.sendMessage(hdmsg);
                        }
                        else{ //invalid id
                            String id = input.readUTF();
                            Message hdmsg = msghandler.obtainMessage();
                            hdmsg.what = 2222;
                            hdmsg.obj = id;
                            msghandler.sendMessage(hdmsg);
                        }
                    }
                //receive gps data
                    else if (msg.equals("gpsData")){
                        String str = input.readUTF();
                        array = str.split("@");
                        String RKlat2 = array[0];
                        String RKlon2 = array[1];

                        if (RKlatitude == null && RKlongitude == null){
                            RKlatitude = RKlat2;
                            RKlongitude = RKlon2;
                        }
                        else if (RKlatitude.equals(RKlat2) && RKlongitude.equals(RKlon2)){
                            Message hdmsg = msghandler.obtainMessage();
                            hdmsg.what = 3333;
                            msghandler.sendMessage(hdmsg);
                    }
                        else{
                            RKlatitude = RKlat2;
                            RKlongitude = RKlon2;

                            if (isLocked == true) {
                                new SendThread(socket, "alarm");
                                new SendThread(socket, "ABC123");

                                Message hdmsg = msghandler.obtainMessage();
                                hdmsg.what = 4444;
                                msghandler.sendMessage(hdmsg);
                            }
                            else {
                                Message hdmsg = msghandler.obtainMessage();
                                hdmsg.what = 3333;
                                msghandler.sendMessage(hdmsg);
                            }
                        }
                    }
                    else{}
                }
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }

//SendThread
    class SendThread extends Thread{
        private Socket socket;
        String sendmsg;
        DataOutputStream output;

        public SendThread(Socket socket, String sendmsg){
            this.socket = socket;
            this.sendmsg = sendmsg;
            try{
                output = new DataOutputStream(socket.getOutputStream());
            }catch (Exception e){

            }
        }

        public void run(){
            try{
                if (output != null){
                    if(sendmsg != null){
                        output.writeUTF(sendmsg);
                    }
                }
            }catch (IOException e){
                e.printStackTrace();
            }catch(NullPointerException npe){
                npe.printStackTrace();
            }
        }
    }
}