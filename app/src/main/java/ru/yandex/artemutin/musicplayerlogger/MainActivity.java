package ru.yandex.artemutin.musicplayerlogger;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        
        Button sendButton = (Button) findViewById(R.id.button_sendLog);
        sendButton.setOnClickListener(new View.OnClickListener() {
                                          @Override
                                          public void onClick(View v) {
                                              //get type of a log trans
                                              //get output format
                                              //make a log in a specified format
                                              //send it with intent
                                              Toast.makeText(v.getContext(), "Send log", Toast.LENGTH_LONG).show();
                                          }
                                      }

        );
    }

}
