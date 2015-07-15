package kr.usis.u_drone;

import android.os.SystemClock;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Context;

import kr.usis.serial.act.DeviceListActivity;

public class MainActivity extends ActionBarActivity {

    TextView textView;
    String a = new String();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = (TextView)findViewById(R.id.textView);
    }

    //button event
    public void onClick(View v){
        DeviceListActivity dla = new DeviceListActivity(this);
        dla.getUSBService();
        dla.refreshDeviceList();

    }

}
