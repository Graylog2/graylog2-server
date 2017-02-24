import Reflux from 'reflux';

const IndicesConfigurationActions = Reflux.createActions({
  loadRotationConfig: { asyncResult: true },
  loadRotationStrategies: { asyncResult: true },
  loadRetentionConfig: { asyncResult: true },
  loadRetentionStrategies: { asyncResult: true },
  updateRotationConfiguration: { asyncResult: true },
  updateRetentionConfiguration: { asyncResult: true },
});

export default IndicesConfigurationActions;
