package com.example.reactive.chapter04;

import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import lombok.extern.slf4j.Slf4j;

/**
 *
 */

@SpringBootApplication
@Slf4j
@EnableAsync
public class Chapter04_1Application {

    @RestController
    public static class MyController{
        Queue<DeferredResult<String>> results = new ConcurrentLinkedQueue<>();

        @GetMapping("/dr")
        public DeferredResult<String> callable() throws InterruptedException {
            log.info("dr");
            DeferredResult<String> dr = new DeferredResult<>(600000L);
            results.add(dr);
            return dr;
        }

        @GetMapping("/dr/count")
        public String drcount(){
            return String.valueOf(results.size());
        }

        @GetMapping("/dr/event")
        public String drevent(String msg){
            for(DeferredResult<String> dr : results){
                dr.setResult("Hello " + msg);
                results.remove(dr);
            }
            return "OK";
        }
//        @GetMapping("/callable")
//        public String callable() throws InterruptedException{
//            log.info("async");
//            Thread.sleep(2000);
//            return "Hello";
//        }

//        public Callable<String> callable() throws InterruptedException {
//            log.info("callable");
//            return () -> {
//                log.info("async");
//                Thread.sleep(2000);
//                return "Hello";
//            };
//        }
    }

    @Component
    public static class MyService{
        @Async
        public Future<String> hello() throws InterruptedException {
            log.info("hello()");
            Thread.sleep(2000);
            return new AsyncResult<>("Hello");
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(Chapter04_1Application.class, args);
    }
}
