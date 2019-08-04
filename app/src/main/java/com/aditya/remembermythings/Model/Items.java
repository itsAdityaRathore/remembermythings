package com.aditya.remembermythings.Model;

public class Items {
    private String Name;
    private String Image;

    public Items(String name, String image) {
        Name = name;
        Image = image;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public String getImage() {
        return Image;
    }

    public void setImage(String image) {
        Image = image;
    }

    public Items() {
    }
}
