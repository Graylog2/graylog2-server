import Reflux from 'reflux';

const HistogramDataActions = Reflux.createActions({
  load: { asyncResult: true },
});

export default HistogramDataActions;
