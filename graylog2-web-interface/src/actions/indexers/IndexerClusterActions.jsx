import Reflux from 'reflux';

const IndexerClusterActions = Reflux.createActions({
  health: { asyncResult: true },
  name: { asyncResult: true },
});

export default IndexerClusterActions;
