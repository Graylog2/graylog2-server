import Reflux from 'reflux';

const PipelineConnectionsActions = Reflux.createActions({
  'list': {asyncResult: true},
  'update': {asyncResult: true},
});

export default PipelineConnectionsActions;
