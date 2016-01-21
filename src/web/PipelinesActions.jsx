import Reflux from 'reflux';

const PipelinesActions = Reflux.createActions({
  'delete': { asyncResult: true },
  'list': { asyncResult: true },
  'get': { asyncResult: true },
  'save': { asyncResult: true },
  'update': { asyncResult: true },
});

export default PipelinesActions;