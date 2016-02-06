import bluebird from 'bluebird';

try {
  /* eslint-disable no-eval */
  eval('Promise');
  /* eslint-enable no-eval */
} catch (e) {
  window.Promise = bluebird;
}

export default Promise;
