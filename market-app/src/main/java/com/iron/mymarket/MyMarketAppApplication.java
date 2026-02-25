package com.iron.mymarket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class MyMarketAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(MyMarketAppApplication.class, args);
	}

}
