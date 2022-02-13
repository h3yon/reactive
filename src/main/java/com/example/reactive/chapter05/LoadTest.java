package com.example.reactive.chapter05;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.util.StopWatch;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LoadTest {
    public static void main(String[] args) throws InterruptedException, BrokenBarrierException {
        AtomicInteger counter = new AtomicInteger(0);

        ExecutorService es = Executors.newFixedThreadPool(100);
        RestTemplate rt = new RestTemplate();
        String url = "http://localhost:8080/rest?idx={idx}";

        //간단하고 쉬운 동기화 방법. 경계를 만들어줌
        CyclicBarrier barrier = new CyclicBarrier(101);

        for(int i = 0; i< 100; i++){
            es.submit(() -> {
                int idx = counter.addAndGet(1);

                //submit으로 exception 이나 리턴값 던질 수 있다.
                barrier.await(); //저 숫자를 만나면 블로킹이 됨 await을 만난 숫자가 100을 만날 때까지 블로킹. 100이 되면 블로킹이 풀려버림. 동기화 가능
                //성능 좀더 향상 가능. 전체 시간 계산하는 것도

               log.info("Thread " + idx);

                StopWatch sw = new StopWatch();
                sw.start();

                String res = rt.getForObject(url, String.class, idx);

                sw.stop();
                log.info("Elapsed: {} {} / {} ", idx, sw.getTotalTimeSeconds(), res);
                return null;
            });
        }

        barrier.await();

        StopWatch main = new StopWatch();
        main.start();

        es.shutdown();
        es.awaitTermination(100, TimeUnit.SECONDS);

        main.stop();
        log.info("Total: {}", main.getTotalTimeSeconds());
    }
}
