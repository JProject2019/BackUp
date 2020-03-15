package com.riderskeeper.riderskeeper;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import androidx.appcompat.app.AppCompatActivity;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import static android.os.SystemClock.sleep;

public class MainActivity extends AppCompatActivity {

    LocationManager locationManager;
    Location location;
    EditText latitude;
    EditText longitude;
    double lat;
    double lon;
    Button reset;
    boolean alarmStatus;

    SocketClient client;
    ReceiveThread receive;
    Socket socket;
    LinkedList<SocketClient> threadList;
    String key = "ABC123";
    String serverIP = "172.30.1.30";
    String serverPort = "8080";

    int sendStat = 0; //0: gps, 1: video

    static final int REQUEST_VIDEO_CAPTURE = 1;
    byte[] byteArray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//initialize EditText with current location
        latitude = findViewById(R.id.lat);
        longitude = findViewById(R.id.lon);
        locationManager = (LocationManager) getApplication().getSystemService(Context.LOCATION_SERVICE);
        updateLocation();
        latitude.setText(String.valueOf(lat));
        longitude.setText(String.valueOf(lon));

//connect to server
        threadList = new LinkedList<MainActivity.SocketClient>();
        client = new SocketClient(serverIP, serverPort);
        threadList.add(client);
        client.start();

//Reset Button - updates current location
        reset = findViewById(R.id.reset);
        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                updateLocation();
                latitude.setText(String.valueOf(lat));
                longitude.setText(String.valueOf(lon));
            }
        });

//periodically send gsp data to server
        Timer timer = new Timer();
        TimerTask TT = new TimerTask() {
            @Override
            public void run() {
                if (sendStat == 0) {
                    updateLocation();
                    new SendThread(socket, latitude.getText().toString() + "@" + longitude.getText().toString()).start();
                }
            }
        };
        timer.schedule(TT, 0, 1000);
    }



    public void updateLocation(){
        location = locationManager.getLastKnownLocation(locationManager.NETWORK_PROVIDER);

        lat = location.getLatitude();
        lon = location.getLongitude();
    }

    public void alarm(){
        alarmStatus = true;
        while (true) {
            if (alarmStatus == true) {
                Vibrator vib = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                vib.vibrate(3000);
                MediaPlayer player = MediaPlayer.create(this, R.raw.beep);
                player.start();
            }
            else break;
            sleep(12000);
        }
    }

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
                    String command = input.readUTF();

                    //alarm
                    if (command.equals("alarm")){
                        Log.e("#######################", "alarm");
                        new Thread(){
                            public void run(){
                                alarm();
                            }
                        }.start();
                    }
                    //alarm off
                    else if(command.equals("alarmOFF")) {
                        alarmStatus = false;
                        Log.e("#######################", "alarm off");
                    }
                    //camera
                    else if (command.equals("camera")){
                        sendStat = 1;
                        dispatchTakeVideoIntent();
                    }
                    //back to sending gps
                    else if (command.equals("location")){
                        sendStat = 0;
                        Log.e("#######################", "sendStat updated");
                    }

                    else{}
                }
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }

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

    private void dispatchTakeVideoIntent() {
        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (takeVideoIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == REQUEST_VIDEO_CAPTURE && resultCode == RESULT_OK) {
            Uri videoUri = intent.getData();
            sendByteArray(videoUri);
        }
    }

    public void sendByteArray(Uri uri){
        String absUri = getRealPathFromURI(uri);
        File file = new File(absUri);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            FileInputStream fis = new FileInputStream(file);
            byte[] buf = new byte[(int)file.length()];
            int n;
            while (-1 != (n = fis.read(buf)))
                baos.write(buf, 0, n);
        }
        catch (IOException e){}

        byteArray = baos.toByteArray();

        new Thread(new Runnable() {
            @Override
            public void run() { // TODO Auto-generated method stub
                try {
                    Socket sock = new Socket("172.30.1.30", 11111);
                    OutputStream os = sock.getOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(os);

                    //send byte[]
                    oos.writeObject(byteArray);
                    oos.close();
                    os.close();
                    sock.close();

                } catch (UnknownHostException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }).start();

    }

    public String getRealPathFromURI(Uri contentUri) {

        String[] proj = { MediaStore.Images.Media.DATA };

        Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
        cursor.moveToNext();
        String path = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DATA));
        Uri uri = Uri.fromFile(new File(path));

        cursor.close();
        return path;
    }
}