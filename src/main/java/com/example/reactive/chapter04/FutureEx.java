package com.example.reactive.chapter04;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FutureEx {
    interface SuccessCallback{
        void onSuccess(String result);
    }
    interface ExceptionCallback{
        void onError(Throwable t);
    }

    public static class CallbackFutureTask extends FutureTask<String>{
        SuccessCallback sc;
        ExceptionCallback ec;

        public CallbackFutureTask(Callable<String> callable, SuccessCallback sc, ExceptionCallback ec) {
            super(callable);
            this.sc = Objects.requireNonNull(sc); //null이 아니면 그냥 리턴
            this.ec = Objects.requireNonNull(ec);
        }

        @Override
        protected void done() { //성공하면 이렇게 할 수 O
            try {
                sc.onSuccess(get());
            }catch (InterruptedException e){
                Thread.currentThread().interrupt();
            }catch (ExecutionException e){
                ec.onError(e.getCause());
            }
        }
    }

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        ExecutorService es = Executors.newCachedThreadPool(); //maximum 제한이 없고 요청할 때마다 새로운 스레드.

        CallbackFutureTask f = new CallbackFutureTask(() -> {
            Thread.sleep(2000);
            if(1==1) throw new RuntimeException("Async Error");
            log.info("Async");
            return "Hello";
        },System.out::println, System.err::println);
//                                                      s -> System.out.println("Result: " + s),
//                                                      e -> System.out.println("Error: " + e.getMessage()));

        es.execute(f);
        es.shutdown();

        //2번
//        //비동기 작업과 결과값을 가진 Future을 담은 거
//        FutureTask<String> f = new FutureTask<String>(() -> {
//            Thread.sleep(2000);
//            log.info("Async");
//            return "Hello";
//        }){
//            //{}에 콜백 가능
//            @SneakyThrows
//            @Override
//            protected void done() {
//                System.out.println(get());
//            }
//        };
//        es.execute(f);
//        es.shutdown();

        //1번
//        Future<String> f = es.submit(() -> {
//                Thread.sleep(2000);
//                log.info("Async");
//                return "Hello";
//        });
//
//        System.out.println(f.isDone()); //Non-Blocking
//        System.out.println(f.get()); //Blocking,
//        log.info("Exit");
    }
}
