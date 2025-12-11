package com.moneyteam;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.*;
//import org.slf4j.simpleLogger.showDateTime=true;
//
//import org.slf4j.simpleLogger.dateTimeFormat=yyyy-MM-dd HH:mm:ss;

@SpringBootApplication(scanBasePackages = "com.moneyteam")
public class Television {
    public static void main(String[] args) {
        SpringApplication.run(Television.class, args);
        System.out.println("DB USER: " + System.getenv("DB_USER"));
    }
}
//
//
//
//}
