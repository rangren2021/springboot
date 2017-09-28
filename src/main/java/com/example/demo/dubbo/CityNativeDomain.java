package com.example.demo.dubbo;

import com.alibaba.dubbo.config.annotation.Service;
import com.example.demo.vo.City;

/**
 * Created by dell on 2017/9/22.
 */
@Service(version = "1.0.0", timeout = 5000, retries = 0)
public class CityNativeDomain implements CityDomain {

//    @Override
    public City getCity(int id) {
        City city = new City();
        city.setId(0);
        city.setCityCode("10001");
        city.setCityName("北京");
        return city;
    }
}
