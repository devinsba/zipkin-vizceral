package com.briandevins;

import org.apache.spark.SparkConf;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import scala.Tuple2;
import zipkin.sparkstreaming.stream.kinesis.KinesisStreamFactory;
import zipkin2.Span;
import zipkin2.codec.SpanBytesDecoder;

import java.util.HashMap;
import java.util.Map;

@Component
public class Job implements CommandLineRunner, AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(Job.class);

    static final int WINDOW_LENGTH_SECONDS = 30;

    @Autowired
    private KinesisStreamFactory streamFactory;

    private JavaStreamingContext jssc;

    public void run(String... strings) throws Exception {
        SparkConf conf = new SparkConf().setMaster("local[2]").setAppName("NetworkWordCount");

        jssc = new JavaStreamingContext(conf, Durations.seconds(1));

        JavaDStream<byte[]> collectedSpans = streamFactory.create(jssc);
        JavaDStream<byte[]> collectedSpansInWindow = collectedSpans.window(Durations.seconds(WINDOW_LENGTH_SECONDS), Durations.seconds(5));
        JavaDStream<Span> spans = collectedSpansInWindow.flatMap(b -> SpanBytesDecoder.JSON_V2.decodeList(b).iterator());

        JavaPairDStream<String, Iterable<Span>> tracesById = spans
                .mapToPair(s -> new Tuple2<>(s.traceId(), s))
                .groupByKey();

        JavaDStream<SpanSummary> spanSummaryJavaDStream = tracesById.flatMap(spanTuples -> {
            Iterable<Span> spansFromTrace = spanTuples._2;
            Map<String, SpanSummary> spanSummaryMap = new HashMap<>();
            for (Span span : spansFromTrace) {
                SpanSummary spanSummary = spanSummaryMap.getOrDefault(span.id(), new SpanSummary());
                if (spanSummary.traceId == null) { // New CallLink
                    spanSummary.traceId = span.traceId();
                    spanSummary.spanId = span.id();
                }

                if (span.kind().equals(Span.Kind.CLIENT)) {
                    spanSummary.from = span.localServiceName();
                    spanSummary.duration = span.duration();

                    if (!spanSummary.to.equals(SpanSummary.DEFAULT_TO)) { // Server span was set up first
                        spanSummary.timestamp = (spanSummary.timestamp + span.timestamp()) / 2;
                    } else {
                        spanSummary.timestamp = span.timestamp();
                    }
                } else if (span.kind().equals(Span.Kind.SERVER)) {
                    spanSummary.to = span.localServiceName();
                    spanSummary.region = span.tags().getOrDefault("aws.region", "us-east-1");
                    spanSummary.env = span.tags().getOrDefault("application.environment", "local");

                    if (!spanSummary.from.equals(SpanSummary.DEFAULT_FROM)) { // Client span was set up first
                        spanSummary.timestamp = (spanSummary.timestamp + span.timestamp()) / 2;
                    } else {
                        spanSummary.timestamp = span.timestamp();
                        spanSummary.duration = span.duration();
                    }
                }
                spanSummaryMap.put(span.id(), spanSummary);
            }

            return spanSummaryMap.values().iterator();
        });

        spanSummaryJavaDStream.foreachRDD(spanSummaryJavaRDD -> SpanSummaryRegistry.update(spanSummaryJavaRDD.collect()));

        jssc.start();
    }

    @Override
    public void close() throws Exception {
        jssc.stop();
        jssc.close();
    }
}
