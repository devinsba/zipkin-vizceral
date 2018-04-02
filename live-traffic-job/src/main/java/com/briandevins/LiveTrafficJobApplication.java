package com.briandevins;

import com.amazonaws.services.kinesis.clientlibrary.lib.worker.InitialPositionInStream;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import zipkin.sparkstreaming.stream.kinesis.KinesisStreamFactory;

@SpringBootApplication
public class LiveTrafficJobApplication {

	public static void main(String[] args) {
		SpringApplication.run(LiveTrafficJobApplication.class, args);
	}

	@Bean
	public KinesisStreamFactory kinesisStreamFactory() {
		return KinesisStreamFactory.newBuilder()
				.stream("zipkin")
				.awsRegion("us-east-1")
				.app("zipkin-kinesis_stream_consumer-test")
				.checkpointIntervalMillis(1000)
				.initialPositionInStream(InitialPositionInStream.LATEST)
				.build();
	}
}
