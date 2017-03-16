import Reflux from 'reflux';

const WidgetsActions = Reflux.createActions({
  removeWidget: { asyncResult: true },
});

export default WidgetsActions;
