import Reflux from 'reflux';

export default Reflux.createActions({
  loadUserPreferences: { asyncResult: true },
  saveUserPreferences: { asyncResult: true },
});
