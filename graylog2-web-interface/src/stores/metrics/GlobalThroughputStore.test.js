// @flow strict
import GlobalThroughputStore from './GlobalThroughputStore';

const expectedThroughput = (input, output) => ({ throughput: { input, output, loading: false } });
const nodeThroughput = (input, output) => ({
  'org.graylog2.throughput.input.1-sec-rate': { metric: { value: input } },
  'org.graylog2.throughput.output.1-sec-rate': { metric: { value: output } },
});
const metricsUpdate = (metrics) => ({ metrics });

const onUpdate = (fn, done) => GlobalThroughputStore.listen((newThroughput) => {
  fn(newThroughput);
  done();
});

describe('GlobalThroughputStore', () => {
  let unsub;

  afterEach(() => {
    if (unsub) { unsub(); }
  });
  it('should return zeroed response if response does not contain metrics', (done) => {
    unsub = onUpdate((newThroughput) => {
      expect(newThroughput).toEqual(expectedThroughput(0, 0));
    }, done);

    GlobalThroughputStore.updateMetrics(metricsUpdate({}));
  });
  it('should extract throughput from response', (done) => {
    unsub = onUpdate((newThroughput) => {
      expect(newThroughput).toEqual(expectedThroughput(42, 17));
    }, done);

    GlobalThroughputStore.updateMetrics(metricsUpdate({
      node1: nodeThroughput(42, 17),
    }));
  });
  it('should sum individual throughputs from response', (done) => {
    unsub = onUpdate((newThroughput) => {
      expect(newThroughput).toEqual(expectedThroughput(609, 5187));
    }, done);

    GlobalThroughputStore.updateMetrics(metricsUpdate({
      node1: nodeThroughput(42, 17),
      node2: nodeThroughput(549, 4980),
      node3: nodeThroughput(18, 190),
    }));
  });
  it('should reset values between sequential updates', (done) => {
    unsub = onUpdate((newThroughput) => {
      expect(newThroughput).toEqual(expectedThroughput(609, 5187));
    }, () => {});

    GlobalThroughputStore.updateMetrics(metricsUpdate({
      node1: nodeThroughput(42, 17),
      node2: nodeThroughput(549, 4980),
      node3: nodeThroughput(18, 190),
    }));
    unsub();

    unsub = onUpdate((newThroughput) => {
      expect(newThroughput).toEqual(expectedThroughput(0, 0));
    }, done);

    GlobalThroughputStore.updateMetrics(metricsUpdate({
      node1: nodeThroughput(0, 0),
      node2: nodeThroughput(0, 0),
      node3: nodeThroughput(0, 0),
    }));
  });
});
