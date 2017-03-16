import Reflux from 'reflux';

const IndexRangesActions = Reflux.createActions({
  list: { asyncResult: true },
  recalculate: { asyncResult: true },
  recalculateIndex: { asyncResult: true },
});

export default IndexRangesActions;
