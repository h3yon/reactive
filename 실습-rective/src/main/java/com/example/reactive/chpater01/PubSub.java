package com.example.reactive.chpater01;

import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class PubSub {

    public static void main(String[] args) throws InterruptedException {
        // Publisher <- Observable
        // Subscriber <- Observer
        Iterable<Integer> iter = Arrays.asList(1, 2, 3, 4, 5);

        ExecutorService es = Executors.newCachedThreadPool();

        Publisher<Integer> p = new Publisher<Integer>() {
            @Override
            public void subscribe(Subscriber<? super Integer> subscriber) {
                Iterator<Integer> it = iter.iterator();

                subscriber.onSubscribe(new Subscription() {
                    @Override
                    public void request(long n) { //동기적: try catch로 잡아서 onError 가능
                        //아래 예제는 진행여부나 결과 상관 없다고 가정
                        //상관 있으면 Future<?> f = es.submit으로 해서 () -> {} 하면 됨. Future는 완전 비동기 처음. 결과가 완료가 되느냐 무엇이냐 작업결과를 담음.
                            //Future는 결과를 받아올 필요가 없음 결과를 이벤트 방식으로 날려버리니까
                            //Future는 작업을 cancel도 할 수 있다. 이걸로 Interrupt 도 가능.
                        es.execute(() -> {
                            int i = 0;
                            while (i++ < n) {
                                if (it.hasNext()) {
                                    subscriber.onNext(it.next());
                                } else {
                                    subscriber.onComplete();
                                    break;
                                }
                            }
                        });
                    }

                    @Override
                    public void cancel() {

                    }
                });
            }
        };

        Subscriber<Integer> s = new Subscriber<Integer>() {
            Subscription subscription;

            @Override
            public void onSubscribe(Subscription subscription) { //내가 subscribe한 그 스레드 안에서 받아서 리퀘스트 onComplete도
                System.out.println(Thread.currentThread().getName() + " onSubscribe");
                this.subscription = subscription;
                this.subscription.request(1); //2개만 달라 의지를 표현함. 10개 보내달라 요청하면 있는 거 5개 다 보내줌
                //여기서 받고 끝나면 안 되니까 멤버변수 해서 this로 정보 넣어
            }

            @Override
            public void onNext(Integer item) {
                System.out.println(Thread.currentThread().getName() + " onNext " + item);
                this.subscription.request(1);
            }

            @Override
            public void onError(Throwable t) {
                System.out.println("onError" + t.getMessage());
            }

            @Override
            public void onComplete() {
                System.out.println(Thread.currentThread().getName() + " onComplete");
            }
        };

        p.subscribe(s);
        es.awaitTermination(10, TimeUnit.HOURS);
        es.shutdown();
    }
}