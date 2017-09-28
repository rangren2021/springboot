package com.example.demo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(scanBasePackages = {"com.example"})  //, excludeName = {"com.exclude.*"}
@EnableTransactionManagement //如果mybatis中service实现类中加入事务注解，需要此处添加该注解
@ServletComponentScan(basePackages = {"com.example.demo.servlet","com.example.demo.filter"})  //扫描Servlet
@MapperScan("com.example.demo.dao.mapper")
@PropertySource({
		"classpath:config/simple.properties",
		"classpath:config/my-web.properties" //如果是相同的key，则最后一个起作用
})
@ImportResource(locations={"classpath:applicationContext-disconf.xml"})
public class SpringbootApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringbootApplication.class, args);
	}
}
