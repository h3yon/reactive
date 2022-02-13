package com.example.reactive.chapter04;

import java.util.concurrent.Future;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@Slf4j
@EnableAsync
public class Chapter04Application {

    @Component
    public static class MyService{
        //ListenableFuture<String>은 f.addCallback(s -> System.out::println, System.err::println) 기다리는 게 아니라 바로 나감
        //Future은 기다리고 그 다음 exit: , result:~ 를 진행함
        //Async로 그냥 하면 SimplePool~이 사용됨. 그건 추천되지 않음
        @Async("tp") //새로운 스레드만 계속 만듦. 실전에선 절대 사용 X. 저렇게 빈으로 등록한 걸로.
        public Future<String> hello() throws InterruptedException{
            log.info("hello");
            Thread.sleep(1000);
            return new AsyncResult<>("Hello");
        }
    }

    @Bean
    ThreadPoolTaskExecutor tp(){
        ThreadPoolTaskExecutor te = new ThreadPoolTaskExecutor();
        te.setCorePoolSize(10); //얘가 꽉 차면 Queue. 큐가 꽉 차면 MaxPoolSize
        te.setMaxPoolSize(100);
        te.setQueueCapacity(200);
        te.setThreadNamePrefix("mythread");
        te.initialize();
        return te;
    }

    public static void main(String[] args) {
        try(ConfigurableApplicationContext c = SpringApplication.run(Chapter04Application.class, args)){

        }
    }

    @Autowired MyService myService;

    @Bean
    ApplicationRunner run(){
        return args -> {
            log.debug("run()");
            Future<String> f = myService.hello();
            log.info("exit: " + f.isDone());
            log.info("result: " + f.get());
        };
    }
}
