package com.bnguimgo.ailoganalyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AiLogAnalyzerApplication {

	public static void main(String[] args) {
		SpringApplication.run(AiLogAnalyzerApplication.class, args);
	}

}
