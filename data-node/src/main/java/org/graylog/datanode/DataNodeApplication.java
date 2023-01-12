package org.graylog.datanode;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DataNodeApplication implements CommandLineRunner {



    public static void main(String[] args) {
        SpringApplication.run(DataNodeApplication.class, args);
    }


    @Override
    public void run(String... args) throws Exception {

    }

}
