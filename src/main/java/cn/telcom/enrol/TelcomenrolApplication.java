package cn.telcom.enrol;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("cn.telcom.enrol.dao")
public class TelcomenrolApplication {

	public static void main(String[] args) {
		SpringApplication.run(TelcomenrolApplication.class, args);
	}

}
