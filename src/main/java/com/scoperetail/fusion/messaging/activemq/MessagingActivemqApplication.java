package com.scoperetail.fusion.messaging.activemq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQAutoConfiguration;

@SpringBootApplication
@EnableAutoConfiguration(exclude={JmsAutoConfiguration.class, ActiveMQAutoConfiguration.class})
public class MessagingActivemqApplication {

	public static void main(String[] args) {
		SpringApplication.run(MessagingActivemqApplication.class, args);
	}

}
