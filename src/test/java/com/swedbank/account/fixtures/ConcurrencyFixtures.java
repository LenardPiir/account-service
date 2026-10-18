package com.swedbank.account.fixtures;

import com.swedbank.account.external.ExternalLoggingClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

public final class ConcurrencyFixtures {

	private static final Duration SLOW_EXTERNAL_CALL = Duration.ofMillis(50);
	private static final Duration MAX_WAIT_PER_CALL = Duration.ofSeconds(30);

	private ConcurrencyFixtures() {
	}

	public static void slowDownExternalLogging(ExternalLoggingClient externalLogging) {
		doAnswer(invocation -> {
			Thread.sleep(SLOW_EXTERNAL_CALL);
			return null;
		}).when(externalLogging).logDebit(any(), any(), any());
	}

	public static <T> Outcomes<T> runConcurrently(int times, Callable<T> call)
			throws InterruptedException, TimeoutException {
		CountDownLatch start = new CountDownLatch(1);
		List<T> results = new ArrayList<>();
		List<Throwable> failures = new ArrayList<>();
		try (ExecutorService executor = Executors.newFixedThreadPool(times)) {
			List<Future<T>> futures = IntStream.range(0, times)
					.mapToObj(i -> executor.submit(() -> {
						start.await();
						return call.call();
					}))
					.toList();
			start.countDown();

			for (Future<T> future : futures) {
				try {
					results.add(future.get(MAX_WAIT_PER_CALL.toMillis(), TimeUnit.MILLISECONDS));
				} catch (ExecutionException e) {
					failures.add(e.getCause());
				}
			}
		}
		return new Outcomes<>(results, failures);
	}

	public record Outcomes<T>(List<T> results, List<Throwable> failures) {
	}

}
