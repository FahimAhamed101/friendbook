package com.app.myfriend.model;

public class ModelPages {

    String id, pId, name, bio, link, username, photo, cover, cat;

    public ModelPages() {
    }

    public ModelPages(String id, String pId, String name, String bio, String link, String username, String photo, String cover, String cat) {
        this.id = id;
        this.pId = pId;
        this.name = name;
        this.bio = bio;
        this.link = link;
        this.username = username;
        this.photo = photo;
        this.cover = cover;
        this.cat = cat;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public String getCover() {
        return cover;
    }

    public void setCover(String cover) {
        this.cover = cover;
    }

    public String getCat() {
        return cat;
    }

    public void setCat(String cat) {
        this.cat = cat;
    }
}
