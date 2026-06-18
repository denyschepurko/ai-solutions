package com.example.chatbot.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.w3c.dom.ls.LSInput;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ChatbotApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ChatbotApiApplication.class, args);
	}

}
