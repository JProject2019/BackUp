package com.riderskeeper.user;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import io.realm.Realm;
import io.realm.RealmResults;

public class edit extends Activity {

    Realm realm;

//onCreate
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.edit_layout);
        this.setFinishOnTouchOutside(false);

//deleteButton OnClickListener
        Button.OnClickListener onClickListener = new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                String s = view.getTag().toString();
                String s2 = s.replace("button", "");

                Toast toast = Toast.makeText(edit.this, "\"" + s2 + "\" deleted", Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL, 0, 0);
                toast.show();

                Intent intent = new Intent();
                intent.putExtra("id", s2);
                setResult(2, intent);
                finish();
            }
        };

//ON/OFF button OnClickListner (2)
        Button.OnClickListener onClickListener2 = new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                String s = view.getTag().toString();
                String s2 = s.replace("button2", "");

                Button b2 = (Button) view;
                String stat = b2.getText().toString();

                if(stat == "OFF"){ //OFF > ON
                    Toast toast = Toast.makeText(edit.this, "\"" + s2 + "\" connected", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL, 0, 0);
                    toast.show();

                    Intent intent = new Intent();
                    intent.putExtra("id", s2);
                    intent.putExtra("color", 1);
                    setResult(3, intent);
                    finish();
                }
                else{ //ON, ERROR > OFF
                    Toast toast = Toast.makeText(edit.this, "\"" + s2 + "\" disconnected", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL, 0, 0);
                    toast.show();

                    Intent intent = new Intent();
                    intent.putExtra("id", s2);
                    intent.putExtra("color", 0);
                    setResult(3, intent);
                    finish();
                }
            }
        };

//scrollView UI
        realm = Realm.getDefaultInstance();
        RealmResults<localDB> results = realm.where(localDB.class).findAll();
        for(localDB RK : results){
            String id = RK.getID();
            appendList(id);

            //layouts - edit_xml
            LinearLayout ll = findViewById(R.id.scrollLinear);
            LinearLayout layer = ll.findViewWithTag("layer" + id);

            //delete button
            Button b = layer.findViewWithTag("button" + id);
            b.setOnClickListener(onClickListener);

            //ON/OFF button
            Button b2 = layer.findViewWithTag("button2" + id);
            b2.setOnClickListener(onClickListener2);

            String st = RK.getStatus();
            if (st.length() == 2){ //green
                b2.setText("ON");
            }else if (st.length() == 5){ //red
                b2.setText("ERROR");
            }else if (st.length() == 3){//red
                b2.setText("OFF");
            }
            else Toast.makeText(edit.this, "Status: " + st, Toast.LENGTH_SHORT).show();
        }

//EditText
        final EditText inputID = findViewById(R.id.ID_input);

//addButton
        Button addButton = findViewById(R.id.add);
        addButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                RealmResults<localDB> results = realm.where(localDB.class).equalTo("ID", inputID.getText().toString()).findAll();

                //already exists
                if (results.size() != 0) {
                    Toast toast = Toast.makeText(edit.this, "\"" + inputID.getText() + "\" already exists", Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL, 0, 0);
                    toast.show();

                    Intent intent = new Intent();
                    intent.putExtra("id", inputID.getText().toString());
                    setResult(0, intent);
                    finish();
                } else {

                    //return input ID
                    Intent intent = new Intent();
                    intent.putExtra("id", inputID.getText().toString());
                    setResult(1, intent);
                    finish();
                }
            }
        });

//cancelButton
        Button cancelButton = findViewById(R.id.cancel);
        cancelButton.setOnClickListener(new View.OnClickListener() {
             public void onClick(View v) {
                 Intent intent = new Intent();
                 intent.putExtra("id", inputID.getText().toString());
                 setResult(0, intent);
                 finish();
             }
         });
    }
//End of onCreate



//appendList
    public void appendList(String id) {

        LinearLayout ll = findViewById(R.id.scrollLinear);

        LinearLayout linear = new LinearLayout(this);
        linear.setTag("layer" + id);
        linear.setOrientation(LinearLayout.HORIZONTAL);
        linear.setGravity(Gravity.RIGHT);
        ll.addView(linear);
        LinearLayout.LayoutParams llparams = (LinearLayout.LayoutParams) linear.getLayoutParams();
        llparams.rightMargin = 80;
        linear.setLayoutParams(llparams);

        TextView text = new TextView(this);
        text.setTag(id);
        text.setText(id + "  ");
        linear.addView(text);

        Button button = new Button(this);
        button.setTag("button" + id);
        button.setText("Delete");
        linear.addView(button);
        ViewGroup.LayoutParams params = button.getLayoutParams();
        params.width = 300;
        params.height = 150;
        button.setLayoutParams(params);

        Button button2 = new Button(this);
        button2.setTag("button2" + id);
        linear.addView(button2);
        ViewGroup.LayoutParams params2 = button2.getLayoutParams();
        params2.width = 200;
        params2.height = 150;
        button2.setLayoutParams(params2);


    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
    }
}