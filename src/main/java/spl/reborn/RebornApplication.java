package spl.reborn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RebornApplication {

	public static void main(String[] args) {
		SpringApplication.run(RebornApplication.class, args);
	}

}
