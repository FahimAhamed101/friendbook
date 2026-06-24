package com.app.myfriend.model;

public class ModelAdsPost {

    String id,pId,text,pViews,type,video,image,pTime,expiry;

    public ModelAdsPost() {
    }

    public ModelAdsPost(String id, String pId, String text, String pViews, String type, String video, String image, String pTime, String expiry) {
        this.id = id;
        this.pId = pId;
        this.text = text;
        this.pViews = pViews;
        this.type = type;
        this.video = video;
        this.image = image;
        this.pTime = pTime;
        this.expiry = expiry;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getpId() {
        return pId;
    }

    public void setpId(String pId) {
        this.pId = pId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getpViews() {
        return pViews;
    }

    public void setpViews(String pViews) {
        this.pViews = pViews;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getVideo() {
        return video;
    }

    public void setVideo(String video) {
        this.video = video;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getpTime() {
        return pTime;
    }

    public void setpTime(String pTime) {
        this.pTime = pTime;
    }

    public String getExpiry() {
        return expiry;
    }

    public void setExpiry(String expiry) {
        this.expiry = expiry;
    }
}
