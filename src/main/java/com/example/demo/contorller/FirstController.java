package com.example.demo.contorller;

import com.example.demo.dao.model.Users;
import com.example.demo.mongodb.dao.UserDao;
import com.example.demo.mongodb.entity.UserEntity;
import com.example.demo.service.UserService;
import com.example.demo.vo.CommonFrom;
import com.example.demo.vo.UserReq;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Random;

/**
 * Created by dell on 2017/9/19.
 */
@RestController
public class FirstController {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private UserService userService;

    @Autowired
    private CommonFrom commonFrom;

    @Autowired
    private UserDao userDao;

    @RequestMapping(value = "/first")
    public String firstFunc() throws Exception {
        System.out.println("firstfunc invoke succ");

        Users users = new Users();
        users.setName("zhangsan");
        users.setPassword("123");
        return mapper.writeValueAsString(users);
    }

    @RequestMapping(value = "/user", method = RequestMethod.POST)
    @ResponseBody
    public Users insertUsers(@RequestBody UserReq data, HttpServletRequest req, HttpServletResponse resp) {
        if (data != null) {
            try {
                Users users = new Users();
                users.setName(data.getName());
                users.setPassword(data.getPassword());
                users.setId(null);
                Integer uid = this.userService.insertUsers(users);
                System.out.println("-----uid:" + uid);

                Users users1 = this.userService.getUsersById(uid);
                System.out.println("users:" + mapper.writeValueAsString(users1));
                return users1;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @RequestMapping(value = "/form", method = RequestMethod.GET)
    @ResponseBody
    public CommonFrom getForm() {
        return commonFrom;
    }

    @RequestMapping(value = "/redis/get", method = RequestMethod.GET)
    @ResponseBody
    public String getRedisInfo(@RequestParam("name") String key) {
        if (StringUtils.hasLength(key)) {
            return this.userService.getRedisInfo(key);
        }
        return "";
    }

    @RequestMapping(value = "/redis/set", method = RequestMethod.POST)
    @ResponseBody
    public String setRedisInfo(@RequestParam("name") String key, @RequestParam("password") String value) {
        if (StringUtils.hasLength(key) && StringUtils.hasLength(value)) {
            return this.userService.setRedisInfo(key, value);
        }
        return "";
    }

    @RequestMapping(value = "/mongo/set", method = RequestMethod.POST)
    @ResponseBody
    public String setMongoInfo(@RequestParam("name") String key, @RequestParam("password") String value) {
        if (StringUtils.hasLength(key) && StringUtils.hasLength(value)) {
            UserEntity entity = new UserEntity();
            entity.setId(new Random().nextInt(10000));
            entity.setPassWord(value);
            entity.setUserName(key);
            this.userDao.saveUser(entity);
            return "ok";
        }
        return "";
    }

    @RequestMapping(value = "/mongo/get", method = RequestMethod.GET)
    @ResponseBody
    public UserEntity getMongoInfo(@RequestParam("name") String key) {
        if (StringUtils.hasLength(key)) {
            return this.userDao.findUserByUserName(key);
        }
        return new UserEntity();
    }

}
