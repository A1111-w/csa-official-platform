package com.csa.official;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@MapperScan("com.csa.official.modules.*.mapper")
public class CsaOfficialApplication {

	public static void main(String[] args) {
		SpringApplication.run(CsaOfficialApplication.class, args);
	}

}
