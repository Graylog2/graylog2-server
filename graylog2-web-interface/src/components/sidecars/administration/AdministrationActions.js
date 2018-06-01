import Reflux from 'reflux';

const AdministrationActions = Reflux.createActions({
  list: { asyncResult: true },
  setAction: { asyncResult: true },
});

export default AdministrationActions;
