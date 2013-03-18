package com.shopzilla.inventory.imp.rdeploader;

import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import com.williamsinteractive.casino.wager.api.OutcomeRequest;
import com.williamsinteractive.casino.wager.api.OutcomeResponse;
import com.williamsinteractive.casino.wager.api.WagerRequest;
import com.williamsinteractive.casino.wager.api.WagerResponse;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * TODO: document!
 *
 * @author Petter Måhlén
 */
public class Loader {

    private static final Supplier<Object> RDEP_REQUEST_SUPPLIER = new WagerRoundSupplier();

    public static void main(String[] args) throws InterruptedException, IOException {
        Configuration configuration = parse(args);

        String url = configuration.url.replaceAll("/$", "");
        JsonClient<WagerRequest, String> wagerClient = new JsonClient<>(String.class, url + "/wager/place");
        JsonClient<OutcomeRequest, String> outcomeClient = new JsonClient<>(String.class, url + "/wager/outcome");

        ExecutorService threadpool = Executors.newFixedThreadPool(configuration.threadCount);

        List<LoadGenerator> generators = Lists.newArrayList();

        for (int i = 0 ; i < configuration.threadCount ; i++) {
            LoadGenerator loadGenerator = new LoadGenerator(wagerClient, outcomeClient, configuration.requestsPerSecond, RDEP_REQUEST_SUPPLIER);
            threadpool.submit(loadGenerator);
            generators.add(loadGenerator);
        }

        if (configuration.timeToRun.isPresent()) {
            Thread.sleep(TimeUnit.SECONDS.toMillis(configuration.timeToRun.get()));

            for (LoadGenerator generator : generators) {
                generator.stop();
            }

            threadpool.shutdown();
            threadpool.awaitTermination(configuration.timeToRun.get(), TimeUnit.SECONDS);
        }
        // otherwise, just keep running till somebody hits control-c
    }

    private static Configuration parse(String[] args) throws IOException {
        OptionParser parser = new OptionParser();

        OptionSpec<Integer> rps = parser.accepts("r").withRequiredArg().ofType(Integer.class).defaultsTo(500).describedAs("Requests/sec");
        OptionSpec<Integer> threadCount = parser.accepts("p").withRequiredArg().ofType(Integer.class).defaultsTo(20).describedAs("Thread Pool size");
        OptionSpec<Integer> timeToRun = parser.accepts("t").withRequiredArg().ofType(Integer.class);
        OptionSpec<String> url = parser.accepts("u").withRequiredArg().ofType(String.class).defaultsTo("http://localhost:8080/").describedAs(
            "URL to send requests to");
        OptionSpec help = parser.accepts("h").forHelp();

        OptionSet options = parser.parse(args);

        if (options.has(help)) {
            parser.printHelpOn(System.out);
            System.exit(0);
        }

        Configuration result = new Configuration();

        result.requestsPerSecond = rps.value(options);
        result.threadCount = threadCount.value(options);
        result.timeToRun = Optional.fromNullable(timeToRun.value(options));
        result.url = url.value(options);

        return result;
    }

    private static class Configuration {
        int requestsPerSecond;
        int threadCount;
        Optional<Integer> timeToRun;
        String url;
    }
}
