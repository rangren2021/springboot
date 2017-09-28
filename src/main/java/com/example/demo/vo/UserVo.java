package com.example.demo.vo;

import org.springframework.data.annotation.Id;

import java.io.Serializable;

/**
 * Created by dell on 2017/9/25.
 */
public class UserVo implements Serializable {

    private static final long serialVersionUID = -3722410814277084698L;

    @Id
    private int id;  //Field
    private String name;
    private int age;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    @Override
    public String toString() {
        return "UserVo{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", age=" + age +
                '}';
    }
}
