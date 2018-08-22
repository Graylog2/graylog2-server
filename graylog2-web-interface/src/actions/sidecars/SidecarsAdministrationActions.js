import Reflux from 'reflux';

const SidecarsAdministrationActions = Reflux.createActions({
  list: { asyncResult: true },
  refreshList: { asyncResult: true },
  setAction: { asyncResult: true },
});

export default SidecarsAdministrationActions;
