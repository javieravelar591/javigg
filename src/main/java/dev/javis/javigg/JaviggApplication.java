package dev.javis.javigg;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;


@SpringBootApplication
public class JaviggApplication {

	private static final Logger log = LoggerFactory.getLogger(JaviggApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(JaviggApplication.class, args);
	}

	// @Bean
	// CommandLineRunner runner() {
		
	// }

}
