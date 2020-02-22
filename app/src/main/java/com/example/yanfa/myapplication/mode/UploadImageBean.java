package com.example.yanfa.myapplication.mode;

public class UploadImageBean {

    private String OBJ_ID;
    private String imageFile;
    private int imageType;

    public UploadImageBean() {
    }

    public UploadImageBean(String imageFile, int imageType) {
        this.imageFile = imageFile;
        this.imageType = imageType;
    }

    public String getImageFile() {
        return imageFile;
    }

    public void setImageFile(String imageFile) {
        this.imageFile = imageFile;
    }

    public int getImageType() {
        return imageType;
    }

    public void setImageType(int imageType) {
        this.imageType = imageType;
    }

    public String getOBJ_ID() {
        return OBJ_ID;
    }

    public void setOBJ_ID(String OBJ_ID) {
        this.OBJ_ID = OBJ_ID;
    }
}
