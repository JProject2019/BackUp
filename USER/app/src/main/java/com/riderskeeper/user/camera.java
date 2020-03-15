package com.riderskeeper.user;

import android.content.Intent;
import android.os.Bundle;
import android.widget.VideoView;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import androidx.appcompat.app.AppCompatActivity;
import android.net.Uri;
import android.os.Environment;
import android.os.Message;
import android.widget.MediaController;
import android.os.Handler;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class camera extends AppCompatActivity {

    VideoView videoView;
    byte[] videodata;
    Uri video;
    Handler myhandler;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.camera_layout);

        Intent intent = new Intent();
        setResult(4, intent);

        videoView = findViewById(R.id.vv);
        MediaController mediaController = new MediaController(this);
        videoView.setMediaController(mediaController);

        myhandler = new Handler() {
            @Override
            public void handleMessage(Message hdmsg) {
                if (hdmsg.what == 1111) {

                    videoView.setVideoURI(video);
                    videoView.start();
                }

            }
        };

        //receive video byteArray
        new Thread() {
            public void run() {
                int port = 12345;
                try {
                    ServerSocket sock = new ServerSocket(port);
                    Socket socket = sock.accept();
                    InputStream is = socket.getInputStream();
                    final ObjectInputStream ois = new ObjectInputStream(is);

                    videodata = (byte[]) ois.readObject();

                    //save as a file
                    InputStream input = new ByteArrayInputStream(videodata);
                    OutputStream output;
                    try {
                        File file = new File (Environment.getExternalStorageDirectory().toString(),"video.mp4");
                        output  = new FileOutputStream(file);
                        byte[] data = new byte[videodata.length];
                        int count;
                        while ((count = input.read(data)) != -1) {
                            output.write(data, 0, count);
                        }
                        output.close();
                        video = Uri.parse(file.getAbsolutePath());

                        Message hdmsg = myhandler.obtainMessage();
                        hdmsg.what = 1111;
                        myhandler.sendMessage(hdmsg);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    ois.close();
                    is.close();
                    socket.close();
                    sock.close();
                } catch (IOException | ClassNotFoundException e) {
                }
            }
        }.start();

    }
}