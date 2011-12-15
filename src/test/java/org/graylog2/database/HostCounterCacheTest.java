package org.graylog2.database;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for {@link HostCounterCache} class
 *
 * @author Andrew NS Yeow <ngeesoon80@yahoo.com>
 */
public class HostCounterCacheTest {
    @Test
    public void testTwoIncrementCallable() throws InterruptedException, ExecutionException, Exception {
        //http://stackoverflow.com/a/3768661

        ExecutorService executor = Executors.newCachedThreadPool();

        final int HOST_COUNT = 10;
        final int LOOP_COUNT = 1000;
        Callable<Map<String,Integer>> getCount1 = new GetAllHostsAndGetCountAndResetCallable();
        Callable<Map<String,Integer>> getCount2 = new GetAllHostsAndGetCountAndResetCallable();
        Callable<int[][]> increment1 = new IncrementCallable(HOST_COUNT, LOOP_COUNT, executor, getCount1);
        Callable<int[][]> increment2 = new IncrementCallable(HOST_COUNT, LOOP_COUNT, executor, getCount2);

        Future<int[][]> incrementSubmit1 = executor.submit(increment1);
        Future<int[][]> incrementSubmit2 = executor.submit(increment2);

        int[][] generatedAndCollectedCounts1 = incrementSubmit1.get();
        int[][] generatedAndCollectedCounts2 = incrementSubmit2.get();

        int[] generatedCounts1 = generatedAndCollectedCounts1[0];
        int[] generatedCounts2 = generatedAndCollectedCounts2[0];
        int[] partailCollectedCounts1 = generatedAndCollectedCounts1[1];
        int[] partailCollectedCounts2 = generatedAndCollectedCounts2[1];
        int[] cumulativeGeneratedCounts = new int[HOST_COUNT];
        int[] cumulativeCollectedCounts = new int[HOST_COUNT];

        for (int i = 0; i < HOST_COUNT; i++) {
            cumulativeGeneratedCounts[i] += generatedCounts1[i];
            cumulativeGeneratedCounts[i] += generatedCounts2[i];
            cumulativeCollectedCounts[i] += partailCollectedCounts1[i];
            cumulativeCollectedCounts[i] += partailCollectedCounts2[i];
        }

        Callable<Map<String,Integer>> getCountFinal = new GetAllHostsAndGetCountAndResetCallable();
        Map<String, Integer> collectedCountsRemainder = getCountFinal.call();
        for (int i = 0; i < HOST_COUNT; i++) {
            String HOST = "host" + i;
            Integer partialCollectedCountRemainder = collectedCountsRemainder.get(HOST);
            if (partialCollectedCountRemainder != null) {
                cumulativeCollectedCounts[i] += partialCollectedCountRemainder.intValue();
            }
        }

        Assert.assertArrayEquals("cumulativeCounts", cumulativeGeneratedCounts, cumulativeCollectedCounts);
    }

    class IncrementCallable implements Callable<int[][]> {
        final int HOST_COUNT;
        final int LOOP_COUNT;
        final ExecutorService executor;
        final Callable<Map<String,Integer>> getCountCallable;

        IncrementCallable(int HOST_COUNT, int LOOP_COUNT, ExecutorService executor, Callable<Map<String,Integer>> getCountCallable) {
            this.HOST_COUNT = HOST_COUNT;
            this.LOOP_COUNT = LOOP_COUNT;
            this.executor = executor;
            this.getCountCallable = getCountCallable;
        }

        @Override
        public int[][] call() throws InterruptedException, ExecutionException {
            final Random random = new Random();
            final int[] generatedCounts = new int[HOST_COUNT];

            final String[] HOSTS = new String[HOST_COUNT];
            for (int i = 0; i < HOST_COUNT; i++) {
                HOSTS[i] = "host" + i;
            }

            Future<Map<String,Integer>> getCountSubmit = null;
            for (int i = 0; i < LOOP_COUNT; i++) {
                if (i == (LOOP_COUNT / 2)) {
                    getCountSubmit = executor.submit(getCountCallable);
                }

                int randomHost = random.nextInt(HOST_COUNT);
                generatedCounts[randomHost]++;
                HostCounterCache.getInstance().increment(HOSTS[randomHost]);
            }

            Map<String,Integer> partialCollectedCounts = getCountSubmit.get();
            int[] intPartialCollectedCounts = new int[HOST_COUNT];
            for (int i = 0; i < HOST_COUNT; i++) {
                Integer partialCollectedCount = partialCollectedCounts.get(HOSTS[i]);
                if (partialCollectedCount != null) {
                    intPartialCollectedCounts[i] += partialCollectedCount.intValue();
                }
            }

            return new int[][] { generatedCounts, intPartialCollectedCounts };
        }
    }

    class GetAllHostsAndGetCountAndResetCallable implements Callable<Map<String,Integer>> {
        @Override
        public Map<String,Integer> call() {
            final Map<String,Integer> collectedCounts = new HashMap<String,Integer>();

            for (String host : HostCounterCache.getInstance().getAllHosts()) {
                int newCount = HostCounterCache.getInstance().getCountAndReset(host);

                if (!collectedCounts.containsKey(host)) {
                    collectedCounts.put(host, newCount);
                } else {
                    int oldCount = collectedCounts.get(host);
                    collectedCounts.put(host, oldCount + newCount);
                }
            }

            return collectedCounts;
        }
    }
}
