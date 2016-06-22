import Reflux from 'reflux';

const AuthenticationActions = Reflux.createActions({
  load: { asyncResult: true },
});

export default AuthenticationActions;
