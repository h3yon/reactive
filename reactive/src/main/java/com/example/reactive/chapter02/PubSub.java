package com.example.reactive.chapter02;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import lombok.extern.slf4j.Slf4j;

// Opertaors
// Publisher -> [Data1] -> Op1 -> [Data2] -> Op2 -> [Data3] -> Subscriber

// Publisher -> [Data1] -> mapPub -> [Data2] -> logSub  //이 방향은 outStream, 역방향은 upStream
//                                   <- subscribe(logSub)
//                                   -> onSubscribe(s) //subscription이 하나 가고
//                                   -> onNext
//                                   -> onNext
//                                   -> onComplete
// Operator Pub을 거쳐야 하기 때문에 subscription을 가져야 한다. 그래서 logSub이 보기엔 mapPub도 퍼블리셔여야 되는 것.

// 1. map (d1 -> f -> d2)

@Slf4j
public class PubSub {

    public static void main(String[] args) throws InterruptedException {

        Publisher<Integer> pub = iterPub(Stream.iterate(1, a -> a + 1).limit(10).collect(Collectors.toList()));
        Publisher<Integer> mapPub = mapPub(pub, s -> s * 10); //얘도 퍼블리셔
        Publisher<Integer> mapPub2 = mapPub(pub, s -> s * -s); // publisher를 하나 더 만들어서 operator할 수 있음
        mapPub2.subscribe(logSub());
    }

    private static Publisher<Integer> mapPub(Publisher<Integer> pub, Function<Integer, Integer> f) {
        return new Publisher<Integer>(){
            @Override
            public void subscribe(Subscriber<? super Integer> sub) {
                pub.subscribe(new Subscriber<Integer>() {
                    @Override
                    public void onSubscribe(Subscription s) { //받아저 작업하고 logSubs의 subscriber한테 줌
                        sub.onSubscribe(s); //중개만 해줌
                    }

                    @Override
                    public void onNext(Integer i) {
                        sub.onNext(f.apply(i)); //function을 적용시킴
                    }

                    @Override
                    public void onError(Throwable t) {
                        sub.onError(t);
                    }

                    @Override
                    public void onComplete() {
                        sub.onComplete();
                    }
                });
            }
        };
    }

    private static Subscriber<Integer> logSub() {
        return new Subscriber<Integer>() {
            Subscription subscription;

            @Override
            public void onSubscribe(Subscription subscription) {
                log.debug("onSubscribe: {}", subscription);
                subscription.request(Long.MAX_VALUE); //정보 다 줘!
            }

            @Override
            public void onNext(Integer item) {
                log.debug("onNext: {}", item);
            }

            @Override
            public void onError(Throwable t) {
                log.debug("onError: {}", t);
            }

            @Override
            public void onComplete() {
                log.debug("onComplete");
            }
        };
    }

    private static Publisher<Integer> iterPub(List<Integer> iter) {
        return new Publisher<Integer>() {
            @Override
            public void subscribe(Subscriber<? super Integer> subscriber) {
                subscriber.onSubscribe(new Subscription() {
                    @Override
                    public void request(long n) { //원래 try-catch를 해주어야 함 catch Throwable하면 sub.onError(t)
                        iter.forEach(s -> subscriber.onNext(s)); //다 줄게
                        subscriber.onComplete();
                    }

                    @Override
                    public void cancel() { //너무 많을 때 더이상 보내지 않아야겠다 subscription을 통해 취소 가능
                    }
                });
            }
        };
    }
}