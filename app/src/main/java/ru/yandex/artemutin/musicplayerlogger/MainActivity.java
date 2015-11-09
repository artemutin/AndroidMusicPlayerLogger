package ru.yandex.artemutin.musicplayerlogger;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Spinner transactionTypes, outputFormats;
        transactionTypes = (Spinner) findViewById(R.id.spinner_logTransaction);
        outputFormats = (Spinner) findViewById(R.id.spinner_outputFormat);

        //look to https://developer.android.com/intl/ru/guide/topics/ui/controls/spinner.html
        ArrayAdapter<CharSequence> transactionTypesAdapter, outputFormatsAdapter;
        transactionTypesAdapter = ArrayAdapter.createFromResource(this, R.array.transactionTypes, android.R.layout.simple_spinner_item);
        outputFormatsAdapter = ArrayAdapter.createFromResource(this, R.array.outputFormats, android.R.layout.simple_spinner_item);
        transactionTypesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        outputFormatsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        transactionTypes.setAdapter(transactionTypesAdapter);
        outputFormats.setAdapter(outputFormatsAdapter);

        Button sendLog = (Button) findViewById(R.id.button_sendLog);
        sendLog.setOnClickListener(new View.OnClickListener() {
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
