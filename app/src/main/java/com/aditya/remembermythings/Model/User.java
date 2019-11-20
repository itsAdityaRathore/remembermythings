package com.aditya.remembermythings.Model;

public class User {
    private String uName;
    private String uPhone;
    private String uPassword;
    private String uSecQues;


    public User() {
    }

    public User(String uName, String uPassword, String uSecQues) {
        this.uName = uName;
        this.uPassword = uPassword;
        this.uSecQues = uSecQues;
    }

    public String getuName() {
        return uName;
    }

    public void setuName(String uName) {
        this.uName = uName;
    }

    public String getuPhone() {
        return uPhone;
    }

    public void setuPhone(String uPhone) {
        this.uPhone = uPhone;
    }

    public String getuPassword() {
        return uPassword;
    }

    public void setuPassword(String uPassword) {
        this.uPassword = uPassword;
    }

    public String getuSecQues() {
        return uSecQues;
    }

    public void setuSecQues(String uSecQues) {
        this.uSecQues = uSecQues;
    }
}
