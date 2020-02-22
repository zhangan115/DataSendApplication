package com.example.yanfa.myapplication;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.example.yanfa.myapplication.blueTooth.BtBase;
import com.example.yanfa.myapplication.blueTooth.BtServer;

public class SendDataActivity extends AppCompatActivity implements View.OnClickListener, BtBase.Listener {

    private BtServer mServer;
    private TextView stateTv, tipTv;
    private boolean isConnected;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_data);
        findViewById(R.id.test_1_btn).setOnClickListener(this);
        findViewById(R.id.test_2_btn).setOnClickListener(this);
        findViewById(R.id.test_3_btn).setOnClickListener(this);
        findViewById(R.id.test_4_btn).setOnClickListener(this);
        stateTv = findViewById(R.id.stateTv);
        tipTv = findViewById(R.id.tipTv);
        mServer = new BtServer(this);
    }

    @Override
    public void onClick(View v) {
        String sendStr = "";
        switch (v.getId()) {
            case R.id.test_1_btn:
                sendStr = "test 1";
                break;
            case R.id.test_2_btn:
                sendStr = "test 2";
                break;
            case R.id.test_3_btn:
                sendStr = "test 3";
                break;
            case R.id.test_4_btn:
                sendStr = "test 4";
                break;
            default:
                break;
        }
        mServer.sendResponse(sendStr);
    }

    @Override
    public void socketNotify(int state, Object obj) {
        if (isDestroyed()) {
            return;
        }
        String msg;
        switch (state) {
            case BtBase.Listener.CONNECTED:
                isConnected = true;
                BluetoothDevice dev = (BluetoothDevice) obj;
                msg = String.format("状态:与(%s)连接成功", dev.getName());
                stateTv.setText(msg);
                mServer.sendMsg("设备连接成功了，我发送了一条数据给你，请查看");
                break;
            case BtBase.Listener.DISCONNECTED:
                isConnected = false;
                msg = "状态:连接断开,请在手机端重新连接";
                stateTv.setText(msg);
                mServer.listen();
                break;
            case BtBase.Listener.RESULT:
                msg = String.format("%s", obj);
                Log.d("za", "==>" + msg);
                break;
            case BtBase.Listener.ERROR:
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
                            SendDataActivity.super.onBackPressed();
                        }
                    }).build().show();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mServer != null) {
            mServer.unListener();
            mServer.close();
        }
    }
}
