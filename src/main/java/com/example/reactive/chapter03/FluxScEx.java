package com.example.reactive.chapter03;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

@Slf4j
public class FluxScEx {

    public static void main(String[] args) throws InterruptedException {
        // 1번 예제
//        Flux.range(1, 10)
//            .publishOn(Schedulers.newSingle("pub"))
//            .log()
//            .subscribeOn(Schedulers.newSingle("sub"))
//            .subscribe(System.out::println);

        //2번 예제
        Flux.interval(Duration.ofMillis(200)) //데몬쓰레드
            .take(10)
            .subscribe(s -> log.debug("onNext: {}", s));
        TimeUnit.SECONDS.sleep(10);
    }
}
