package kr.usis.u_drone;

import java.nio.ByteBuffer;

/**
 * Created by Daniel on 2015-07-28.
 */
public class ThreadChSend extends Thread {
    byte[] data = new byte[50];
    byte[] buff;
    private final ByteBuffer mWriteBuffer = ByteBuffer.allocate(4096);
    public void run() {
        while (true) {
            if (!StateBuffer.BufferStorage.isEmpty()) {
                data = StateBuffer.BufferStorage.poll();
                while (StateBuffer.flagThread_ch_send_Run) {
                    try {
                        mWriteBuffer.put(data);
                        synchronized (mWriteBuffer) {
                            int len = mWriteBuffer.position();
                            if (len > 0) {
                                buff = new byte[50];
                                mWriteBuffer.rewind();
                                mWriteBuffer.get(buff, 0, len);
                                mWriteBuffer.clear();
                            }
                        }
                        if (buff != null) {
                            StateBuffer.CONNECTION.write(buff, 1000);
                        }
                    } catch (Exception e) {
                    }
                    try {  Thread.sleep(20);} catch (InterruptedException e) { }

                }
            }
        }
    }
}
