package com.example.yanfa.myapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.example.yanfa.myapplication.app.DataApp;
import com.example.yanfa.myapplication.blueTooth.BtBase;
import com.example.yanfa.myapplication.blueTooth.BtClient;
import com.example.yanfa.myapplication.mode.CheckType;
import com.example.yanfa.myapplication.mode.UploadImageBean;
import com.example.yanfa.myapplication.mode.db.ReceiveData;
import com.example.yanfa.myapplication.mode.db.ReceiveData_;
import com.example.yanfa.myapplication.utlis.BtReceiver;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.objectbox.android.AndroidScheduler;
import io.objectbox.reactive.DataObserver;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements BtBase.Listener, EasyPermissions.PermissionCallbacks, BtReceiver.Listener, BtDevAdapter.Listener {
    private RecyclerView recyclerView;
    private RecyclerView deviceRecycleView;
    private final BtDevAdapter mBtDevAdapter = new BtDevAdapter(this);
    private TextView stateTv, tipTv;
    private List<ReceiveData> dataList = new ArrayList<>();
    private List<UploadImageBean> needUploadImage = new ArrayList<>();
    private final List<BluetoothDevice> mDevices = new ArrayList<>();
    private boolean isConnected;
    private final int REQUEST_ENABLE_BT = 200;
    private final int REQUEST_EXTERNAL = 100;
    private BtClient btClient;
    private String blueToothPhoto;
    private BtReceiver mBtReceiver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        mBtReceiver = new BtReceiver(this, this);//注册蓝牙广播
        checkPermission();
        loadDataFromDb();
    }

    @AfterPermissionGranted(REQUEST_EXTERNAL)
    private void initData() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            Toast.makeText(this, "当前设备不支持蓝牙", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!adapter.isEnabled()) {
            //打开蓝牙
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            return;
        }
        createPhotoDir();
        makeBluetoothDiscover();
        if (btClient == null) {
            btClient = new BtClient(this);
            stateTv.setText("状态:服务启动，等待仪器连接......");
        }
    }

    private void loadDataFromDb() {
        DataApp.getInstance().getBoxStore().boxFor(ReceiveData.class).query()
                .orderDesc(ReceiveData_.time)
                .build()
                .subscribe()
                .on(AndroidScheduler.mainThread())
                .observer(new DataObserver<List<ReceiveData>>() {
                    @Override
                    public void onData(@NonNull List<ReceiveData> data) {
                        if (isDestroyed()) return;
                        MainActivity.this.dataList.clear();
                        MainActivity.this.dataList.addAll(data);
                        if (recyclerView.getAdapter() != null)
                            recyclerView.getAdapter().notifyDataSetChanged();
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_OK) {
            initData();
        }
    }

    private void createPhotoDir() {
        blueToothPhoto = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "blueToothPhoto";
        File file = new File(blueToothPhoto);
        if (!file.exists()) {
            file.mkdir();
        }
    }

    //蓝牙可供检测到
    private void makeBluetoothDiscover() {
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120);
        startActivity(discoverableIntent);
    }

    private void initView() {
        stateTv = findViewById(R.id.stateTv);
        tipTv = findViewById(R.id.tipTv);
        stateTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPermission();
            }
        });
        deviceRecycleView = findViewById(R.id.deviceRecycleView);
        deviceRecycleView.setLayoutManager(new LinearLayoutManager(this));
        deviceRecycleView.setAdapter(mBtDevAdapter);
        recyclerView = findViewById(R.id.recycleView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        Adapter adapter = new Adapter(this);
        recyclerView.setAdapter(adapter);
        findViewById(R.id.clean_all_tv).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DataApp.getInstance().getBoxStore().boxFor(ReceiveData.class).removeAll();
                loadDataFromDb();
            }
        });
        findViewById(R.id.titleTv).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SendDataActivity.class));
            }
        });
        findViewById(R.id.device).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBtDevAdapter.reScan();
            }
        });
    }

    @Override
    public void socketNotify(int state, Object obj) {
        if (isDestroyed())
            return;
        String msg;
        switch (state) {
            case BtBase.Listener.CONNECTED:
                isConnected = true;
                BluetoothDevice dev = (BluetoothDevice) obj;
                msg = String.format("状态:与(%s)连接成功", dev.getName());
                stateTv.setText(msg);
                break;
            case BtBase.Listener.DISCONNECTED:
                isConnected = false;
                msg = "状态:连接断开,请重新连接";
                stateTv.setText(msg);
                break;
            case BtBase.Listener.RESULT:
                //字符消息
                msg = String.format("%s", obj);
                try {
                    JSONObject json = new JSONObject(msg);
                    String str = json.getString("data");
                    String type = json.getString("type");
                    Log.d("za", "type == " + type);
                    Log.d("za", "str == " + str);
                    ReceiveData receiveData = new ReceiveData();
                    receiveData.state = 0;
                    receiveData.time = System.currentTimeMillis();
                    receiveData.checkType = type;
                    switch (type) {
                        case CheckType.TGP://特高频
                            receiveData.dataName = "特高频检测数据";
                            receiveData.jsonData = str;
                            break;
                        case CheckType.CSB://超声波
                            receiveData.dataName = "超声波检测数据";
                            receiveData.jsonData = str;
                            break;
                        case CheckType.HW://红外
                            receiveData.dataName = "红外检测数据";
                            receiveData.jsonData = str;
                            break;
                        default:
                            DataApp.toast("暂不支持该类型的检测数据!", Toast.LENGTH_SHORT);
                            break;
                    }
                    if (!TextUtils.isEmpty(receiveData.jsonData)) {
                        //保持该数据
                        DataApp.getInstance().getBoxStore().boxFor(ReceiveData.class).put(receiveData);
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("errorCode", 0);
                        jsonObject.put("type", BtBase.FLAG_MSG);
                        btClient.sendResponse(jsonObject.toString());//数据接收成功 将结果发送到仪器端
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    JSONObject jsonObject = new JSONObject();
                    try {
                        jsonObject.put("errorCode", 1);
                        jsonObject.put("type", 0);
                        jsonObject.put("message", "Json格式错误");
                    } catch (JSONException e1) {
                        e1.printStackTrace();
                    }
                    btClient.sendResponse(jsonObject.toString());
                }
                break;
            case BtBase.Listener.ERROR:
                //错误
                break;
            case BtBase.Listener.MSG:
                //消息
                msg = String.format("提示:%s", obj);
                tipTv.setText(msg);
                break;
        }
    }

    @Override
    public void onBackPressed() {
        if (isConnected) {
            new MaterialDialog.Builder(this).content("正在与仪器连接，是否退出?")
                    .negativeText("取消").positiveText("确定")
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            dialog.dismiss();
                            MainActivity.super.onBackPressed();
                        }
                    }).build().show();
        } else {
            super.onBackPressed();
        }
    }

    void checkPermission() {
        if (!EasyPermissions.hasPermissions(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                , Manifest.permission.BLUETOOTH
                , Manifest.permission.BLUETOOTH_ADMIN
                , Manifest.permission.ACCESS_COARSE_LOCATION)) {
            stateTv.setText("点击申请权限");
            EasyPermissions.requestPermissions(this, getString(R.string.request_permissions),
                    REQUEST_EXTERNAL, android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    , Manifest.permission.BLUETOOTH
                    , Manifest.permission.BLUETOOTH_ADMIN
                    , Manifest.permission.ACCESS_COARSE_LOCATION);
        } else {
            initData();
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (btClient != null) {
            btClient.unListener();
            btClient.close();
        }
        unregisterReceiver(mBtReceiver);
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        if (requestCode == REQUEST_EXTERNAL) {
            initData();
        }
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this)
                    .setRationale(getString(R.string.need_save_setting))
                    .setTitle(getString(R.string.request_permissions))
                    .setPositiveButton(getString(R.string.sure))
                    .setNegativeButton(getString(R.string.cancel))
                    .setRequestCode(REQUEST_EXTERNAL)
                    .build()
                    .show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void foundDev(BluetoothDevice dev) {
        mBtDevAdapter.add(dev);
    }

    @Override
    public void onItemClick(BluetoothDevice dev) {
        if (btClient.isConnected(dev)) {
            Toast.makeText(this, "已经连接了", Toast.LENGTH_SHORT).show();
            return;
        }
        btClient.connect(dev);
        Toast.makeText(this, "正在连接", Toast.LENGTH_SHORT).show();
        stateTv.setText("正在连接...");
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {

        Adapter(Context context) {
            this.context = context;
        }

        private Context context;

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            @SuppressLint("InflateParams")
            View view = LayoutInflater.from(context).inflate(R.layout.item_data_list, null);
            ViewHolder holder = new ViewHolder(view);
            holder.nameTv = view.findViewById(R.id.dataNameTv);
            holder.timeTv = view.findViewById(R.id.timeTv);
            holder.view = view.findViewById(R.id.view);
            return holder;
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.nameTv.setText(dataList.get(position).dataName);
            holder.itemView.setTag(position);
            holder.timeTv.setText(timeFormat(dataList.get(position).time, null));
        }

        @Override
        public int getItemCount() {
            if (dataList == null) {
                return 0;
            }
            return dataList.size();
        }
    }

    private class ViewHolder extends RecyclerView.ViewHolder {

        TextView nameTv;
        TextView timeTv;
        View view;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    /**
     * 格式化时间
     *
     * @param time   时间
     * @param format 格式化
     * @return 结果
     */
    public static String timeFormat(long time, @Nullable String format) {
        if (format == null || format.isEmpty()) {
            format = "yyyy-MM-dd HH:mm:ss";
        }
        Date d = new Date(time);
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
        return sdf.format(d);
    }
}
