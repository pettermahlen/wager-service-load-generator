package com.shopzilla.inventory.imp.rdeploader;

import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.williamsinteractive.casino.wager.api.OutcomeRequest;
import com.williamsinteractive.casino.wager.api.WagerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

/**
 * TODO: document!
 *
 * @author Petter Måhlén
 */
public class LoadGenerator implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(LoadGenerator.class);
    private static final Random RANDOM = new SecureRandom();
    public static final int LOG_INTERVAL = 500;
    private final JsonClient<WagerRequest, String> client;
    private final JsonClient<OutcomeRequest, String> outcomeClient;
    private final int requestsPerSecond;
    private final Supplier<Object> requestGenerator;
    private volatile boolean stop = false;

    private int requestCount = 0;

    public LoadGenerator(JsonClient<WagerRequest, String> client,
                         JsonClient<OutcomeRequest, String> outcomeClient,
                         int requestsPerSecond,
                         Supplier<Object> requestGenerator) {
        Preconditions.checkArgument(requestsPerSecond < 1000, "Cannot handle more than 1000 rps");
        this.client = client;
        this.outcomeClient = outcomeClient;
        this.requestsPerSecond = requestsPerSecond;
        this.requestGenerator = requestGenerator;
    }

    @Override
    public void run() {
        LOG.info("Starting LoadGenerator with client {} and rps {}", client, requestsPerSecond);

        long lastTimestamp = System.nanoTime();
        while (!stop) {
            try {
                Object request = requestGenerator.get();

                if (request instanceof WagerRequest) {
                    client.call((WagerRequest) request);
                }
                else {
                    outcomeClient.call((OutcomeRequest) request);
                }
                requestCount++;
                LOG.debug("Sent request {}", request);

                if (requestCount % LOG_INTERVAL == 0) {
                    long now = System.nanoTime();
                    long duration = now - lastTimestamp;
                    double rps = (double) TimeUnit.SECONDS.toNanos(LOG_INTERVAL) / (double) duration;
                    LOG.info("Sent {} requests in {} ms for an rps of {}", LOG_INTERVAL, NANOSECONDS.toMillis(duration), rps);
                    lastTimestamp = now;
                }

                Thread.sleep(millisToSleep());
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void stop() {
        stop = true;
    }

    private long millisToSleep() {
        long averageSleepMicros = 1000000 / requestsPerSecond;

        long randomFactor = averageSleepMicros * 2; // make it go from 0 to average * 2.
        long middle = averageSleepMicros;

        long randomness = RANDOM.nextLong() % randomFactor;

        return Math.max(0, TimeUnit.MICROSECONDS.toMillis(averageSleepMicros + randomness - middle));
    }
}
