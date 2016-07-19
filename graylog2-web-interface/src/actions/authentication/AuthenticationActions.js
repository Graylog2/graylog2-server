import Reflux from 'reflux';

const AuthenticationActions = Reflux.createActions({
  load: { asyncResult: true },
  update: { asyncResult: true },
});

export default AuthenticationActions;
