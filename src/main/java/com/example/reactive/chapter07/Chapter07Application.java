package com.example.reactive.chapter07;

import java.util.concurrent.CompletableFuture;
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

import com.example.reactive.chapter06.Chapter06Application.Completion;

import io.netty.channel.nio.NioEventLoopGroup;

@EnableAsync
@SpringBootApplication
public class Chapter07Application {
    @RestController
    public static class MyController{
        @Autowired MyService myService;

        static final String URL1 = "http://localhost:8081/service?req={req}";
        static final String URL2 = "http://localhost:8081/service2?req={req}";

        AsyncRestTemplate rt = new AsyncRestTemplate(new Netty4ClientHttpRequestFactory(new NioEventLoopGroup(1)));

        @GetMapping("/rest")
        public DeferredResult<String> rest(int idx){
            DeferredResult<String> dr = new DeferredResult<>();

//            Completion.from(rt.getForEntity(URL1, String.class, "hello" + idx))
//                      .andApply(s -> rt.getForEntity(URL2, String.class, s.getBody()))
//                      .andApply(s -> myService.work(s.getBody()))
//                      .andError(e -> dr.setErrorResult(e.toString()))
//                      .andAccept(s -> dr.setResult(s));
            toCF(rt.getForEntity(URL1, String.class, "hello" + idx))
                .thenCompose(s -> toCF( rt.getForEntity(URL2, String.class, s.getBody())))
                .thenApply(s2 -> myService.work(s2.getBody()))
                .thenAccept(s3 -> dr.setResult(s3))
                .exceptionally(e -> {dr.setErrorResult(e.getMessage()); return (Void)null;});


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

    public static <T> CompletableFuture<T> toCF(ListenableFuture<T> lf){ //명시적으로 비동기 작업이 성공, 실패했을 때를 알려줄 수 있음
        CompletableFuture<T> cf = new CompletableFuture<T>();
        lf.addCallback(s -> cf.complete(s), e -> cf.completeExceptionally(e));
        return cf;
    }

    public static class AcceptCompletion<S, T> extends Completion<S, Void> {
        Consumer<S> con;
        public AcceptCompletion(Consumer<S> con) {
            this.con = con;
        }

        @Override
        public void run(S value) {
            con.accept(value); //작업 처리해 라면서 이전 작업 넘겨줌
        }
    }

    public static class ErrorCompletion<T>extends Completion<T, T>{
        Consumer<Throwable> econ;
        public ErrorCompletion(Consumer<Throwable> econ) {
            this.econ = econ;
        }

        @Override
        public void run(T value) {
            if(next != null) next.run(value);
        }

        @Override
        public void error(Throwable ex) {
            econ.accept(ex); //내가 처리할게
        }
    }

    public static class ApplyCompletion<S, T> extends Completion<S, T>{
        Function<S, ListenableFuture<T>> fn;
        public ApplyCompletion(Function<S, ListenableFuture<T>> fn) {
            this.fn = fn;
        }

        @Override
        public void run(S value) {
            ListenableFuture<T> lf = fn.apply(value);
            lf.addCallback(s -> complete(s), ex -> error(ex));
        }
    }

    public static class Completion<S, T>{
        Completion next;

        public void andAccept(Consumer<T> con){
            Completion<T, Void> completion = new AcceptCompletion<>(con);
            this.next = completion;
        }

        public Completion<T,T> andError(Consumer<Throwable> econ){
            Completion<T,T> completion = new ErrorCompletion<>(econ);
            this.next = completion;
            return completion;
        }

        public <V> Completion<T, V> andApply(Function<T, ListenableFuture<V>> fn){
            Completion<T, V> completion = new ApplyCompletion<>(fn);
            this.next = completion;
            return completion;
        }

        public static <S, T> Completion<S, T> from(ListenableFuture<T> lf) {
            Completion<S, T> completion = new Completion();
            lf.addCallback(s -> {
                completion.complete(s);
            }, ex -> {
                completion.error(ex);
            });
            return completion;
        }

        public void error(Throwable ex) {
            if(next != null) next.error(ex);
        }

        public void complete(T s) {
            if(next != null) next.run(s);
        }

        public void run(S value) { }
    }

    @Service
    public static class MyService{
        public String work(String req){
            return req + "/asyncwork";
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
        SpringApplication.run(Chapter07Application.class, args);
    }
}
