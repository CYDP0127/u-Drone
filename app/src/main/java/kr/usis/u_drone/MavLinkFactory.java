package kr.usis.u_drone;

import android.widget.Toast;

import org.mavlink.MAVLinkReader;
import org.mavlink.messages.MAVLinkMessage;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Created by Daniel on 2015-07-20.
 */
public class MavLinkFactory {

    public MAVLinkMessage readMavlink(byte[] data) throws IOException {
        DataInputStream e = new DataInputStream(new ByteArrayInputStream(data));
        MAVLinkReader reader = new MAVLinkReader(e, (byte)-2);
        MAVLinkMessage msg = null;

        try{
            msg = reader.getNextMessage();

        } catch (Exception var6) {  ;  }
        e.close();
        return msg;
    }
}