package ch.admin.bit.jeap.archrepo.web;

import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableAsync
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
public class ArchRepoApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArchRepoApplication.class, args);
    }

}
