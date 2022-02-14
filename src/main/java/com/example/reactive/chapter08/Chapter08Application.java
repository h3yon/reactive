package com.example.reactive.chapter08;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.Netty4ClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.channel.nio.NioEventLoopGroup;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@SpringBootApplication
@EnableAsync
@RestController
public class Chapter08Application {

    static final String URL1 = "http://localhost:8081/service?req={req}";
    static final String URL2 = "http://localhost:8081/service2?req={req}";

    @Autowired
    MyService myService;

    WebClient client = WebClient.create();

    @GetMapping("/rest")
    public Mono<String> rest(int idx){ //Mono로 리턴하면 subscribe를 알아서 호출해줌. 그럼 다 알아서 동작
        return client.get().uri(URL1, idx).exchange().flatMap(clientResponse -> clientResponse.bodyToMono(String.class))
                     .doOnNext(c -> log.info(c.toString()))
                     .flatMap(res1 -> client.get().uri(URL2, res1).exchange())
                     .flatMap(c -> c.bodyToMono(String.class))
                     .doOnNext(c -> log.info(c.toString()))
                     .flatMap(res2 -> Mono.fromCompletionStage(myService.work(res2))); //String을 뽑아와서 리턴값도 String이니까. 시간 걸리는 작업이면 또다른 거에서 해야함 이때 Completable 사용해보기
    }

    //왜 나는 스레드가 1개는 아니지....
    public static void main(String[] args) {
        System.setProperty("reactor.ipc.netty.workerCount", "1");
        System.setProperty("reactor.ipc.netty.pool.maxConnections", "2000");
        SpringApplication.run(Chapter08Application.class, args);
    }

    @Service
    public static class MyService{
        @Async
        public CompletableFuture<String> work(String req){
            return CompletableFuture.completedFuture(req + "/asyncwork");
        }
    }
}
