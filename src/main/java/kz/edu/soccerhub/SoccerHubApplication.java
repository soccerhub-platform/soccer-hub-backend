package kz.edu.soccerhub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class SoccerHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(SoccerHubApplication.class, args);
    }

}
