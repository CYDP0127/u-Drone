package kr.usis.u_drone;

import java.nio.ByteBuffer;

/**
 * Created by 최용득(Daniel) on 2015-07-28.
 */

//변경된 체널값 및 모드 설정 등을 송신하기위한 쓰레드
public class ThreadChSend extends Thread {
    byte[] data = new byte[50];
    byte[] buff;
    private final ByteBuffer mWriteBuffer = ByteBuffer.allocate(4096);
    public void run() {
        while (true) {
            if (!StateBuffer.BufferStorage.isEmpty()) {
                data = StateBuffer.BufferStorage.poll();            //송신될 데이터를 저장하는 queue에서 데이터를 꺼내옴(byte 배열)
                while (StateBuffer.flagThread_ch_send_Run) {        //다음 데이터가 있을때까지 계속해서 보냄 sleep 20ms
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
                            StateBuffer.CONNECTION.write(buff, 1000); //송신
                        }
                    } catch (Exception e) {
                    }
                    try {  Thread.sleep(20);} catch (InterruptedException e) { }

                }
            }
        }
    }
}
