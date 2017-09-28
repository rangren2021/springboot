package com.example.demo.vo;

import java.io.Serializable;

/**
 * Created by dell on 2017/9/28.
 */
public class UserReq implements Serializable{

    private static final long serialVersionUID = -4228085978837653915L;
    private String name;
    private String password;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
