package de.isuret.polos.AetherOnePi;

import de.isuret.polos.AetherOnePi.processing.AetherOneProcessingMain;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@SpringBootApplication
@EnableAsync
public class AetherOnePiApplication {

	public static void main(String[] args) {

		if (args.length > 0 && args[0].equals("processing")) {
			AetherOneProcessingMain.main(AetherOneProcessingMain.class.getName());
		} else {
			SpringApplication.run(AetherOnePiApplication.class, args);
		}
	}

    @Bean
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("AetherOnePi-");
        executor.initialize();
        return executor;
    }
}

