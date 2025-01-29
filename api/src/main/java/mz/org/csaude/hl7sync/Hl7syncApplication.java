package mz.org.csaude.hl7sync;

import mz.org.csaude.hl7sync.env.EncryptedEnvironmentLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class Hl7syncApplication {

	public static void main(String[] args) {
		new SpringApplicationBuilder(Hl7syncApplication.class)
				.listeners(new EncryptedEnvironmentLoader())
				.run(args);
	}

}
