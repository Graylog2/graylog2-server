/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import { GlobalThroughputStore } from './GlobalThroughputStore';

const expectedThroughput = (input, output) => ({ throughput: { input, output, loading: false } });
const nodeThroughput = (input, output) => ({
  'org.graylog2.throughput.input.1-sec-rate': { metric: { value: input } },
  'org.graylog2.throughput.output.1-sec-rate': { metric: { value: output } },
});
const metricsUpdate = (metrics) => ({ metrics });

const onUpdate = (fn) =>
  new Promise<void>((resolve) => {
    const unsub = GlobalThroughputStore.listen((newThroughput) => {
      fn(newThroughput);
      unsub();
      resolve();
    });
  });

describe('GlobalThroughputStore', () => {
  it('should return zeroed response if response does not contain metrics', async () => {
    const promise = onUpdate((newThroughput) => {
      expect(newThroughput).toEqual(expectedThroughput(0, 0));
    });

    GlobalThroughputStore.updateMetrics(metricsUpdate({}));

    await promise;
  });

  it('should extract throughput from response', async () => {
    const promise = onUpdate((newThroughput) => {
      expect(newThroughput).toEqual(expectedThroughput(42, 17));
    });

    GlobalThroughputStore.updateMetrics(
      metricsUpdate({
        node1: nodeThroughput(42, 17),
      }),
    );

    await promise;
  });

  it('should sum individual throughputs from response', async () => {
    const promise = onUpdate((newThroughput) => {
      expect(newThroughput).toEqual(expectedThroughput(609, 5187));
    });

    GlobalThroughputStore.updateMetrics(
      metricsUpdate({
        node1: nodeThroughput(42, 17),
        node2: nodeThroughput(549, 4980),
        node3: nodeThroughput(18, 190),
      }),
    );

    await promise;
  });

  it('should reset values between sequential updates', async () => {
    const firstPromise = onUpdate((newThroughput) => {
      expect(newThroughput).toEqual(expectedThroughput(609, 5187));
    });

    GlobalThroughputStore.updateMetrics(
      metricsUpdate({
        node1: nodeThroughput(42, 17),
        node2: nodeThroughput(549, 4980),
        node3: nodeThroughput(18, 190),
      }),
    );

    await firstPromise;

    const secondPromise = onUpdate((newThroughput) => {
      expect(newThroughput).toEqual(expectedThroughput(0, 0));
    });

    GlobalThroughputStore.updateMetrics(
      metricsUpdate({
        node1: nodeThroughput(0, 0),
        node2: nodeThroughput(0, 0),
        node3: nodeThroughput(0, 0),
      }),
    );

    await secondPromise;
  });
});
