import Reflux from 'reflux';

const SessionActions = Reflux.createActions([
    'login',
    'loggedIn',
    'logout',
    'loggedOut',
]);

export default SessionActions;
