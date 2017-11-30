import Reflux from 'reflux';

const ClusterTrafficActions = Reflux.createActions({
  traffic: { asyncResult: true },
});

export default ClusterTrafficActions;
