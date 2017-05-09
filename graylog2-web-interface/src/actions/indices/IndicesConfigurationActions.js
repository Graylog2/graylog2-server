import Reflux from 'reflux';

const IndicesConfigurationActions = Reflux.createActions({
  loadRotationStrategies: { asyncResult: true },
  loadRetentionStrategies: { asyncResult: true },
});

export default IndicesConfigurationActions;
