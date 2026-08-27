package com.csa.official;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CsaOfficialApplicationTests {

	@Autowired
	private Environment environment;

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void contextLoads() {
		assertThat(environment.getActiveProfiles()).contains("test");
		assertThat(applicationContext.getBeansOfType(RedisConnectionFactory.class)).isEmpty();
	}

}
