package com.example.demo.vo;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * Created by dell on 2017/9/21.
 */

@PropertySource("classpath:config/my-web.properties")
@ConfigurationProperties(prefix = "web")
@Component
public class CommonFrom implements Serializable {

    private static final long serialVersionUID = -221974179654017136L;

    private String name;
    private String age;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }
}
