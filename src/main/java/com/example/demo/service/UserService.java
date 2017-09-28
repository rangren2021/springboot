package com.example.demo.service;

import com.example.demo.dao.mapper.UsersMapper;
import com.example.demo.dao.model.Users;
import com.example.demo.redis.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Created by dell on 2017/9/19.
 */
@Service
@Transactional
public class UserService {

    @Autowired
    private UsersMapper usersMapper;

    @Autowired
    private RedisUtil redisUtil;

    public Users getUsersById(Integer id) {
        if (!StringUtils.isEmpty(id)) {
            return this.usersMapper.selectByPrimaryKey(id);
        }
        return null;
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, timeout = 36000, rollbackFor = Exception.class)
    public Integer insertUsers(Users users) {
        int res = this.usersMapper.insertSelective(users);
        return users.getId();
    }

    public String getRedisInfo(String key){
        return this.redisUtil.getString(key);
    }

    public String setRedisInfo(String key, String value){
        return this.redisUtil.setString(key, value);
    }


}
