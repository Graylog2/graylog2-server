import Reflux from 'reflux';

const SessionActions = Reflux.createActions({
  'login': {asyncResult: true},
  'logout': {asyncResult: true},
});

export default SessionActions;
