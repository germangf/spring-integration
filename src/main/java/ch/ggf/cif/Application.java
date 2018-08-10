package ch.ggf.cif;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableAutoConfiguration
@SpringBootApplication
@EnableDiscoveryClient
public class Application {

	public static void main(String[] args) {
		System.setProperty("spring.config.name", "cif-statement");
		SpringApplication.run(Application.class, args);
	}
}
