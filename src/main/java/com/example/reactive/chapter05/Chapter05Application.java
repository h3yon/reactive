package com.example.reactive.chapter05;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.Netty4ClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.async.DeferredResult;

import io.netty.channel.nio.NioEventLoopGroup;

@EnableAsync
@SpringBootApplication
public class Chapter05Application {
    @RestController
    public static class MyController{
        @Autowired MyService myService;

        static final String URL1 = "http://localhost:8081/service?req={req}";
        static final String URL2 = "http://localhost:8081/service2?req={req}";

        AsyncRestTemplate rt = new AsyncRestTemplate(new Netty4ClientHttpRequestFactory(new NioEventLoopGroup(1)));

        @GetMapping("/rest")
        public DeferredResult<String> rest(int idx){
            DeferredResult<String> dr = new DeferredResult<>();

            //2초 동안 대기 안 하게 됨. Listen~을 사용했으니까 블로킹 -> 블로킹X
            ListenableFuture<ResponseEntity<String>> f1 = rt
                .getForEntity(URL1, String.class, "hello" + idx);
//            return "rest " + idx; //view-name => hello.jsp / test/html
            //CPU 차지하는 건 정말 많은 자원을 필요로 함. 블로킹 작업은 반드시 컨텍스트 스위칭이 2번씩 일어남.
            //스레드 갯수를 올리면 한계에 다다르면 메모리가 풀이 되거나 etc

            f1.addCallback(s -> {
                ListenableFuture<ResponseEntity<String>> f2 = rt
                    .getForEntity(URL2, String.class, s.getBody());
                f2.addCallback(s2 -> {
                    ListenableFuture<String> f3 = myService.work(s2.getBody());
                    f3.addCallback(s3 -> {
                        dr.setResult(s3);
                    }, ex -> {
                        dr.setErrorResult(ex.getMessage());
                    });
                }, ex -> {
                        dr.setErrorResult(ex.getMessage());
                       });
            }, e -> {
                dr.setErrorResult(e.getMessage());
            });

            return dr;
        }
    }

    @Service
    public static class MyService{
        @Async
        public ListenableFuture<String> work(String req){
            return new AsyncResult<>(req + "/asyncwork");
        }
    }

    @Bean
    public ThreadPoolTaskExecutor myThreadPool(){
        ThreadPoolTaskExecutor te = new ThreadPoolTaskExecutor();
        te.setCorePoolSize(1);
        te.setMaxPoolSize(1);
        te.initialize();
        return te;
    }

    public static void main(String[] args) {
        SpringApplication.run(Chapter05Application.class, args);
    }
}
