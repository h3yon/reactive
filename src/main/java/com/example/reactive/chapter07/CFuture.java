package com.example.reactive.chapter07;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CFuture {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        ExecutorService es = Executors.newFixedThreadPool(10);

        //예시1. CompletableFuture
        //Future: java5 이후에 동작. 비동기 작업 결과를 담고 있는 object. get으로 결과를 가져왔음. 콜백으로 결과 가져옴. 결과 안 가져왔으면 대기 예외면 예외 던짐
//        CompletableFuture<Integer> f = CompletableFuture.completedFuture(1); //비동기작업을 직접 완료함. 이미 작업 완료된 CompletableFuture을 그냥 만들 수 O
//        CompletableFuture<Integer> f = new CompletableFuture<>(); //완료하지 않아서 무한정 대기할 것
//        f.completeExceptionally(new RuntimeException()); //이렇게 끝내줄 수 있다. ex)f.complete(2)
//        System.out.println(f.get()); //호출될 때 사용 가능. 얘도 블러킹돼서 콜백을 선호하긴 함

        //예시2. 백그라운드 스레드를 하나 만든 다음에 거기서 수행해라라는 코드
        CompletableFuture
//            .runAsync(() -> log.info("runAsync")) //그 ForkJoinPool의 스레드에서 다음 출력. 다음의 thenRun에서 결과값을 사용을 못함
//            .thenRun(() -> log.info("thenRun")) //같은 쓰레드에서

            //만약에 다른 스레드에서 아래 작업을 다르게 하고 싶다? -> thenApplyAsync를 사용
            .supplyAsync(() -> { //백그라운드 스레드 만들어짐
                log.info("runAsync");
                return 1;
            })
            .thenApply(s -> { //thenApply를 쓰면 앞에 온 값 받아서 쓸 수 O. 같은 스레드
                log.info("thenRun {}", s);
                return s+1; //CompletableFuture.completedFuture(s+1) 이렇게 functional return type이 적용되면 **.thenCompose로 바꿔주자
            })
            .exceptionally(e -> -10) //예외 발생하면 순차적으로 받아서 -10으로 받아서 넘겨줌
            .thenAccept(s2 -> log.info("thenRun {}", s2));

            //exception이 발생하면 무시하고 무시하고 끝남
        log.info("exit"); //얘가 먼저 출력
        ForkJoinPool.commonPool().shutdown();
        ForkJoinPool.commonPool().awaitTermination(10, TimeUnit.SECONDS);
    }
}
