package com.riderskeeper.user;

import io.realm.RealmObject;

public class localDB extends RealmObject{

    private String ID;
    private String status;
    private String imageURI;

    public String getID(){
        return ID;
    }

    public void setID(String id){
        this.ID = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String stat) {
        this.status = stat;
    }

    public String getImageURI(){
        return imageURI;
    }

    public void setImageURI(String imageUri){
        this.imageURI = imageUri;
    }

}