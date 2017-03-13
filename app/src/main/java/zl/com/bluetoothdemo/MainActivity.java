package zl.com.bluetoothdemo;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemClickListener {

    private static final int REQUEST_PERMISSION_ACCESS_LOCATION = 1;
    private ListView mList;
    private ArrayList<BluetoothDevice> strArr;
    private BluetoothAdapter blueToothAdapter;
    private ListViewAdapter adapter;
    private ToggleButton mSwitch;
    private Button mGetBindDevice;
    private Button mSearch;
    private UUID uuid = UUID.fromString("00001106-0000-1000-8000-00805F9B34FB");
    private Button mStartService;
    private TextView state;
    private static final int startService = 0;
    private static final int getMessageOk = 1;
    private static final int sendOver = 2;


    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case startService:
                    state.setText("服务已打开");
                    break;
                case getMessageOk:
                    state.setText(msg.obj.toString());
                    break;
                case sendOver:
                    Toast.makeText(MainActivity.this, "发送成功", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        blueToothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (blueToothAdapter != null) {
            state = (TextView) findViewById(R.id.state);
            mGetBindDevice = (Button) findViewById(R.id.getBindDevice);
            mGetBindDevice.setOnClickListener(this);
            mSearch = (Button) findViewById(R.id.search);
            mSearch.setOnClickListener(this);
            mStartService = (Button) findViewById(R.id.startService);
            mStartService.setOnClickListener(this);
            mSwitch = (ToggleButton) findViewById(R.id.btn_switch);
            mSwitch.setOnClickListener(this);
            //打开app的时候首先获取蓝牙的状态，如果是打开的，那么设置mSwitch是关闭蓝牙状态
            mSwitch.setChecked(!blueToothAdapter.isEnabled());
            mList = (ListView) findViewById(R.id.list);
            strArr = new ArrayList<>();
            adapter = new ListViewAdapter();
            mList.setAdapter(adapter);
            mList.setOnItemClickListener(this);

            //注册广播
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
            intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
            registerReceiver(new BluetoothReceiver(), intentFilter);
        }
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            int checkAccessFinePermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
            if (checkAccessFinePermission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_PERMISSION_ACCESS_LOCATION);
                Log.e(getPackageName(), "没有权限，请求权限");
                return;
            }
            Log.e(getPackageName(), "已有定位权限");
            search();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.e(getPackageName(), "开启权限permission granted!");
                    //做下面该做的事
                    search();
                } else {
                    Log.e(getPackageName(), "没有定位权限，请先开启!");
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    public void search() {
        if (blueToothAdapter.isDiscovering())
            blueToothAdapter.cancelDiscovery();

        blueToothAdapter.startDiscovery();
        Log.e(getPackageName(), "开始搜索");
    }

    public void getBindDevice() {
        Set<BluetoothDevice> bondedDevices = blueToothAdapter.getBondedDevices();
        strArr.clear();
        strArr.addAll(bondedDevices);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_switch:
                if (mSwitch.isChecked()) {
                    blueToothAdapter.disable();
                } else {
                    //第一种打开方式
                    blueToothAdapter.enable();
                    //第二种打开方式
//                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//                    startActivityForResult(intent, 1);
                    //第三种打开方式
//                    Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
//                    discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
//                    startActivity(discoverableIntent);
                }
                break;
            case R.id.getBindDevice:
                getBindDevice();
                break;
            case R.id.search:
                requestPermission();
                break;
            case R.id.startService:
                getMessage();
                break;
        }

    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, final int i, long l) {
        if (blueToothAdapter.isDiscovering())
            blueToothAdapter.cancelDiscovery();
        if (strArr.get(i).getBondState() == BluetoothDevice.BOND_NONE) {
            bond(i);
        } else if (strArr.get(i).getBondState() == BluetoothDevice.BOND_BONDED) {
            sendMessage(i);
        }
    }

    private void getMessage() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                InputStream is = null;
                try {
                    BluetoothServerSocket serverSocket = blueToothAdapter.listenUsingRfcommWithServiceRecord("serverSocket", uuid);
                    mHandler.sendEmptyMessage(startService);
                    BluetoothSocket accept = serverSocket.accept();
                    is = accept.getInputStream();

                    byte[] bytes = new byte[1024];
                    int length = is.read(bytes);

                    Message msg = new Message();
                    msg.what = getMessageOk;
                    msg.obj = new String(bytes, 0, length);
                    mHandler.sendMessage(msg);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void sendMessage(final int i) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                OutputStream os = null;
                try {
                    BluetoothSocket socket = strArr.get(i).createRfcommSocketToServiceRecord(uuid);
                    socket.connect();
                    os = socket.getOutputStream();
                    os.write("testMessage".getBytes());
                    os.flush();
                    mHandler.sendEmptyMessage(sendOver);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }


    private void bond(int i) {
        try {
            Method method = BluetoothDevice.class.getMethod("createBond");
            Log.e(getPackageName(), "开始配对");
            method.invoke(strArr.get(i));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    class ListViewAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return strArr.size();
        }

        @Override
        public Object getItem(int i) {
            return strArr.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            view = LayoutInflater.from(MainActivity.this).inflate(android.R.layout.simple_list_item_1, viewGroup, false);
            BluetoothDevice device = strArr.get(i);
            ((TextView) view).setText(device.getName() + "-----" + (device.getBondState() == BluetoothDevice.BOND_BONDED ? "已绑定" : "未绑定"));
            return view;
        }
    }

    class BluetoothReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(BluetoothDevice.ACTION_FOUND)) {
                Log.e(getPackageName(), "找到新设备了");
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                boolean addFlag = true;
                for (BluetoothDevice bluetoothDevice : strArr) {
                    if (device.getAddress().equals(bluetoothDevice.getAddress())) {
                        addFlag = false;
                    }
                }

                if (addFlag) {
                    strArr.add(device);
                    adapter.notifyDataSetChanged();
                }
            } else if (intent.getAction().equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                switch (device.getBondState()) {
                    case BluetoothDevice.BOND_NONE:
                        Log.e(getPackageName(), "取消配对");
                        break;
                    case BluetoothDevice.BOND_BONDING:
                        Log.e(getPackageName(), "配对中");
                        break;
                    case BluetoothDevice.BOND_BONDED:
                        Log.e(getPackageName(), "配对成功");
                        break;
                }


            }
        }
    }


}
