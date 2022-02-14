package com.example.reactive.chapter09;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@SpringBootApplication
@RestController //클래스 자체를 빈으로 등록해줌
public class Chapter09Application {

    @GetMapping("/")
    Mono<String> hello(){
        //Mono로 리턴한다는 것 = publisher가 만들어서 subscribe해줘서 data 받게 한다
        //먼저 subscribe()하면 동작하지 않는다.
//        return Mono.just("Hello WebFlux").doOnNext(c -> log.info(c)).log(); // Publisher -> (Publisher) -> (Publisher) -> Subscriber

        //subscribe 여러가지 가질 수 있음
        //퍼블리싱하는 소스: cold / hot type 있다. 요청하든지 동일한 결과는 cold type, 새 구독할 때마다 퍼블리셔가 있는 거 처음부터 줌
        //hot: 외부시스템에서 실시간으로 바뀌는 정보가 있다. 이런 거. 구독하는 시점부터 실시간
        log.info("position1");
        Mono<String> mono = Mono.fromSupplier(() -> "Hello").doOnNext(c -> log.info(c)).log();
        mono.subscribe(); //위에 모노 동작함
        String msg2 = mono.block();
        log.info("position2" + msg2);
//        return mono; //다시 mono 반복됨. 최종 결과값 Mono.just(msg2) 하면 결과값만 딱 보내준다!
        return Mono.just(msg2);
    }

    public static void main(String[] args) {
        SpringApplication.run(Chapter09Application.class, args);
    }
}
