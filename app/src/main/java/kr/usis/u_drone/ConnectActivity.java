package kr.usis.u_drone;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

/**
 * Created by msnl on 7/15/15.
 */
public class ConnectActivity extends Activity {
    Button ConnectButton;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.connection_layout);
        Toast.makeText(ConnectActivity.this, "Connection Established", Toast.LENGTH_SHORT).show();
    }
}
