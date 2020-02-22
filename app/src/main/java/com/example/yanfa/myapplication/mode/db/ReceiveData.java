package com.example.yanfa.myapplication.mode.db;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

@Entity
public class ReceiveData {
    @Id
    public long id;
    public String jsonData;
    public String checkType;//检测类型
    public int state;// 状态，0 未上传成功 1 上传成功
    public long serviceId;//服务器Id
    public long time; // 时间
    public String dataName; //
    public String OBJ_ID;//
}
