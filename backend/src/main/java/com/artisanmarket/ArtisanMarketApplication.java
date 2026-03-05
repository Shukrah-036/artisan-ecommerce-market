package com.artisanmarket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class ArtisanMarketApplication {

	public static void main(String[] args) {
		SpringApplication.run(ArtisanMarketApplication.class, args);
	}

}
