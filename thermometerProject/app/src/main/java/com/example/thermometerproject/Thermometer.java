package com.example.thermometerproject;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Thermometer extends AppCompatActivity implements View.OnClickListener{

    ImageView thermometer;
    ImageView humiditymeter;
    ImageView imgConnect;
    TextView textConnect;
    TextView textV;
    TextView textA;
    LinearLayout connectMenu;
    LinearLayout sendMenu;
    private static final int REQUEST_ENABLE_BT = 10; // 블루투스 활성화 상태
    private BluetoothAdapter bluetoothAdapter; // 블루투스 어댑터
    private Set<BluetoothDevice> devices; // 블루투스 디바이스 데이터 셋
    private BluetoothDevice bluetoothDevice; // 블루투스 디바이스
    private BluetoothSocket bluetoothSocket = null; // 블루투스 소켓
    private OutputStream outputStream = null; // 블루투스에 데이터를 출력하기 위한 출력 스트림
    private InputStream inputStream = null; // 블루투스에 데이터를 입력하기 위한 입력 스트림
    private Thread workerThread = null; // 문자열 수신에 사용되는 쓰레드
    private byte[] readBuffer; // 수신 된 문자열을 저장하기 위한 버퍼
    private int readBufferPosition; // 버퍼 내 문자 저장 위치
    private int pariedDeviceCount; // 페어링 된 Device 숫자 세기

    float temp = 0;  // 받아올 온도 센서 값
    float hum = 0;  // 받아올 습도 센서 값

    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.thermometer);
        thermometer = (ImageView)findViewById(R.id.thermometer);
        humiditymeter = (ImageView)findViewById(R.id.humiditymeter);
        textV = (TextView)findViewById(R.id.textV);
        textA = (TextView)findViewById(R.id.texta);
        connectMenu = (LinearLayout)findViewById(R.id.btnConnect);
        sendMenu = (LinearLayout)findViewById(R.id.btnSend);
        connectMenu.setOnClickListener((View.OnClickListener) this);
        sendMenu.setOnClickListener((View.OnClickListener) this);
        imgConnect = (ImageView)findViewById(R.id.imgConnect);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter(); // 기본 어댑터로 설정
        if(bluetoothAdapter == null) // 블루투스 지원하지 않을 때
        {
            //empty
        }
        else { // 디바이스가 블루투스를 지원 할 때
            if(bluetoothAdapter.isEnabled())
            { // 블루투스가 활성화 상태 (기기에 블루투스가 켜져있음)
                selectBluetoothDevice(); // 블루투스 디바이스 선택 함수 호출
            }
            else { // 블루투스가 비 활성화 상태 (기기에 블루투스가 꺼져있음)
                // 블루투스를 활성화 하기 위한 다이얼로그 출력
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                // 선택한 값이 onActivityResult 함수에서 콜백된다.
                startActivityForResult(intent, REQUEST_ENABLE_BT);
            }
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_ENABLE_BT :
                if(requestCode == RESULT_OK) { // '사용'을 눌렀을 때
                    selectBluetoothDevice(); // 블루투스 디바이스 선택 함수 호출
                }
                else { // '취소'를 눌렀을 때
                    // 여기에 처리 할 드를 작성하세요.
                }
                break;
        }
    }


    public void selectBluetoothDevice() {
        // 이미 페어링 되어있는 블루투스 기기를 찾습니다.
        devices = bluetoothAdapter.getBondedDevices();
        // 페어링 된 디바이스의 크기를 저장
        pariedDeviceCount = devices.size();
        // 페어링 되어있는 장치가 없는 경우
        if(pariedDeviceCount == 0) {
            // 페어링을 하기위한 함수 호출
        }
        // 페어링 되어있는 장치가 있는 경우
        else {
            // 디바이스를 선택하기 위한 다이얼로그 생성
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("페어링 되어있는 블루투스 디바이스 목록");
            // 페어링 된 각각의 디바이스의 이름과 주소를 저장
            List<String> list = new ArrayList<>();
            // 모든 디바이스의 이름을 리스트에 추가
            for(BluetoothDevice bluetoothDevice : devices) {
                list.add(bluetoothDevice.getName());
            }
            list.add("취소");
            // List를 CharSequence 배열로 변경
            final CharSequence[] charSequences = list.toArray(new CharSequence[list.size()]);
            list.toArray(new CharSequence[list.size()]);

            // 해당 아이템을 눌렀을 때 호출 되는 이벤트 리스너
            builder.setItems(charSequences, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // 해당 디바이스와 연결하는 함수 호출
                    connectDevice(charSequences[which].toString());
                }
            });

            // 뒤로가기 버튼 누를 때 창이 안닫히도록 설정
            builder.setCancelable(false);
            // 다이얼로그 생성
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        }
    }

    public void connectDevice(String deviceName) {
        // 페어링 된 디바이스들을 모두 탐색
        for(BluetoothDevice tempDevice : devices) {
            // 사용자가 선택한 이름과 같은 디바이스로 설정하고 반복문 종료
            if(deviceName.equals(tempDevice.getName())) {
                bluetoothDevice = tempDevice;
                break;
            }
        }
        // UUID 생성
        UUID uuid = java.util.UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
        // SPP 통신
        try {
            bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
            bluetoothSocket.connect();
            // 블루투스 소켓 연결
            imgConnect.setImageResource(R.drawable.connect); // 이미지뷰 초록색으로 변경
            // 데이터 송,수신 스트림
            outputStream = bluetoothSocket.getOutputStream();
            inputStream = bluetoothSocket.getInputStream();
            // 데이터 수신 함수 호출
            final Handler handler = new Handler();
            // 데이터를 수신하기 위한 버퍼를 생성
            readBufferPosition = 0;
            readBuffer = new byte[1024];
            // 데이터를 수신하기 위한 쓰레드 생성
            workerThread = new Thread(new Runnable() {
                @Override
                public void run() {
//                    while(Thread.currentThread().isInterrupted()) {
                        while(true) {
                            try {
                            // 데이터를 수신했는지 확인합니다.
                            int byteAvailable = inputStream.available();
                            textV.setText(Integer.toString(byteAvailable));
                                //데이터가 수신 된 경우
                            if(byteAvailable > 0) {
                                // 입력 스트림에서 바이트 단위로 읽어 옵니다.
                                byte[] bytes = new byte[byteAvailable];
                                inputStream.read(bytes);
                                // 입력 스트림 바이트를 한 바이트씩 읽어 옵니다.

                                for(int i = 0; i < byteAvailable; i++) {
                                    byte tempByte = bytes[i];
                                    // 개행문자를 기준으로 받음(한줄)
                                    if(tempByte == '\n') {

                                        // readBuffer 배열을 encodedBytes로 복사
                                        byte[] encodedBytes = new byte[readBufferPosition];
                                        System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                        // 인코딩 된 바이트 배열을 문자열로 변환
                                        final String text = new String(encodedBytes, "US-ASCII");
                                        readBufferPosition = 0;

                                        handler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                // 텍스트 뷰에 출력
                                                textA.setText(text + "\n");
                                            }
                                        });
                                    } // 개행 문자가 아닐 경우
                                    else {
                                        readBuffer[readBufferPosition++] = tempByte;
                                    }
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        try {
                            // 1초마다 받아옴
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            workerThread.start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    void sendData(String text) {
        // 문자열에 개행문자("\n")를 추가해줍니다.
        text += "\n";
        try{
            // 데이터 송신
            outputStream.write(text.getBytes());
        }catch(Exception e) {
            e.printStackTrace();
        }

    }



//    public void receiveData() {
//        final Handler handler = new Handler();
//        // 데이터를 수신하기 위한 버퍼를 생성
//        readBufferPosition = 0;
//        readBuffer = new byte[1024];
//        // 데이터를 수신하기 위한 쓰레드 생성
//        workerThread = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                while(Thread.currentThread().isInterrupted()) {
//                    try {
//                        // 데이터를 수신했는지 확인합니다.
//                        int byteAvailable = inputStream.available();
//                        // 데이터가 수신 된 경우
//                        if(byteAvailable > 0) {
//                            // 입력 스트림에서 바이트 단위로 읽어 옵니다.
//                            byte[] bytes = new byte[byteAvailable];
//                            inputStream.read(bytes);
//                            // 입력 스트림 바이트를 한 바이트씩 읽어 옵니다.
//                            for(int i = 0; i < byteAvailable; i++) {
//                                byte tempByte = bytes[i];
//                                // 개행문자를 기준으로 받음(한줄)
//                                if(tempByte == '\n') {
//                                    // readBuffer 배열을 encodedBytes로 복사
//                                    byte[] encodedBytes = new byte[readBufferPosition];
//                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
//                                    // 인코딩 된 바이트 배열을 문자열로 변환
//                                    final String text = new String(encodedBytes, "US-ASCII");
//                                    readBufferPosition = 0;
//                                    handler.post(new Runnable() {
//                                        @Override
//                                        public void run() {
//                                            // 텍스트 뷰에 출력
//                                            textV.append(text + "\n");
//                                        }
//                                    });
//                                } // 개행 문자가 아닐 경우
//                                else {
//                                    readBuffer[readBufferPosition++] = tempByte;
//                                }
//                            }
//                        }
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                    try {
//                        // 1초마다 받아옴
//                        Thread.sleep(1000);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        });
//        workerThread.start();
//    }




    @Override
    public void onClick(View v)
    {
        if(v.getId()==R.id.btnConnect) // 다시 연결하고 싶을 때
        {
            imgConnect.setImageResource(R.drawable.disconnect); // 이미지뷰 빨간색으로 변경
            if(bluetoothAdapter.isEnabled())
            { // 블루투스가 활성화 상태라면
                selectBluetoothDevice(); // 블루투스 디바이스 선택 함수 호출
            }
        }
        else if(v.getId()==R.id.btnSend)
        {
            textV.setText("시발");
//            receiveData();
            sendData("okay");
        }
    }





}

//        if(temp<10)
//            thermometer.setImageResource(R.drawable.thermometer4);
//        else if(temp<20)
//            thermometer.setImageResource(R.drawable.thermometer3);
//        else if(temp<30)
//            thermometer.setImageResource(R.drawable.thermometer2);
//        else
//            thermometer.setImageResource(R.drawable.thermometer1);
//
//        if(hum<10)
//            humiditymeter.setImageResource(R.drawable.humiditymeter1);
//        else if(hum<20)
//            humiditymeter.setImageResource(R.drawable.humiditymeter2);
//        else if(hum<30)
//            humiditymeter.setImageResource(R.drawable.humiditymeter3);
//        else
//            humiditymeter.setImageResource(R.drawable.humiditymeter4);