package com.example.reactive.chapter06;

import java.util.function.Consumer;
import java.util.function.Function;

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
import org.springframework.web.context.request.async.DeferredResult;

import io.netty.channel.nio.NioEventLoopGroup;

@EnableAsync
@SpringBootApplication
public class Chapter06Application {
    @RestController
    public static class MyController{
        @Autowired MyService myService;

        static final String URL1 = "http://localhost:8081/service?req={req}";
        static final String URL2 = "http://localhost:8081/service2?req={req}";

        AsyncRestTemplate rt = new AsyncRestTemplate(new Netty4ClientHttpRequestFactory(new NioEventLoopGroup(1)));

        @GetMapping("/rest")
        public DeferredResult<String> rest(int idx){
            DeferredResult<String> dr = new DeferredResult<>();

            Completion.from(rt.getForEntity(URL1, String.class, "hello" + idx))
                    .andApply(s -> rt.getForEntity(URL2, String.class, s.getBody()))
                    .andAccept(s -> dr.setResult(s.getBody()));
//            ListenableFuture<ResponseEntity<String>> f1 = rt.getForEntity(URL1, String.class, "hello" + idx);
//            f1.addCallback(s -> {
//                ListenableFuture<ResponseEntity<String>> f2 = rt
//                    .getForEntity(URL2, String.class, s.getBody());
//                f2.addCallback(s2 -> {
//                    ListenableFuture<String> f3 = myService.work(s2.getBody());
//                    f3.addCallback(s3 -> {
//                        dr.setResult(s3);
//                    }, ex -> {
//                        dr.setErrorResult(ex.getMessage());
//                    });
//                }, ex -> {
//                        dr.setErrorResult(ex.getMessage());
//                       });
//            }, e -> {
//                dr.setErrorResult(e.getMessage());
//            });

            return dr;
        }
    }

    public static class Completion{
        Completion next;

        public Completion() { }

        Consumer<ResponseEntity<String>> con;
        public Completion(Consumer<ResponseEntity<String>> con) {
            this.con = con;
        }

        Function<ResponseEntity<String>, ListenableFuture<ResponseEntity<String>>> fn;
        public Completion(Function<ResponseEntity<String>, ListenableFuture<ResponseEntity<String>>> fn) {
            this.fn = fn;
        }

        public void andAccept(Consumer<ResponseEntity<String>> con){
            Completion completion = new Completion(con);
            this.next = completion;
        }

        public Completion andApply(Function<ResponseEntity<String>, ListenableFuture<ResponseEntity<String>>> fn){
            Completion completion = new Completion(fn);
            this.next = completion;
            return completion;
        }

        public static Completion from(ListenableFuture<ResponseEntity<String>> lf) {
            Completion completion = new Completion();
            lf.addCallback(s -> {
                completion.complete(s);
            }, ex -> {
                completion.error(ex);
            });
            return completion;
        }

        private void error(Throwable ex) {

        }

        private void complete(ResponseEntity<String> s) {
            if(next != null) next.run(s);
        }

        private void run(ResponseEntity<String> value) {
            if(con != null) con.accept(value); //받아서 넘기도
            else if(fn != null){ //run이면 어떤 작업을 해주어야 함
                ListenableFuture<ResponseEntity<String>> lf = fn.apply(value);
                lf.addCallback(s -> complete(s), ex -> error(ex)); //이 비동기 작업에 대해서도 콜백을 해주어야 함
            }
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
        SpringApplication.run(Chapter06Application.class, args);
    }
}
