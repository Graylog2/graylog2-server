import Reflux from 'reflux';

const FieldQuickValuesActions = Reflux.createActions({
  get: { asyncResult: true },
  getHistogram: { asyncResult: true },
});

export default FieldQuickValuesActions;
