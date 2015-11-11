import Reflux from 'reflux';

const IndexRangesActions = Reflux.createActions({
  'list': { asyncResult: true },
  'recalculate': { asyncResult: true },
});

export default IndexRangesActions;
