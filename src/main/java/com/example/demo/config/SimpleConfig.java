package com.example.demo.config;

import com.baidu.disconf.client.common.annotations.DisconfFile;
import com.baidu.disconf.client.common.annotations.DisconfFileItem;
import com.baidu.disconf.client.common.update.IDisconfUpdate;
import org.springframework.context.annotation.Configuration;

/**
 * Created by dell on 2017/9/26.
 */
@Configuration
@DisconfFile(filename = "config/simple.properties")
public class SimpleConfig implements IDisconfUpdate {


    private int id;
    private String company;
    private String phone;

    @DisconfFileItem(name = "simple.id", associateField = "id")
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @DisconfFileItem(name = "simple.company", associateField = "company")
    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    @DisconfFileItem(name = "simple.phone", associateField = "phone")
    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    @Override
    public void reload() throws Exception {
        System.out.println("SimpleConfig: " + this.toString());
    }

    @Override
    public String toString() {
        return "SimpleConfig{" +
                "id=" + id +
                ", company='" + company + '\'' +
                ", phone='" + phone + '\'' +
                '}';
    }

}
