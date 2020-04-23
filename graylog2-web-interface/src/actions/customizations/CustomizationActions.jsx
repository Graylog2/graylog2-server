import Reflux from 'reflux';

const CustomizationActions = Reflux.createActions({
  update: { asyncResult: true },
  get: { asyncResult: true },
});

export default CustomizationActions;
