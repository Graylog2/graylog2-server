import Reflux from 'reflux';

export default Reflux.createActions({
  get: {
    asyncResult: true,
  },
  execute: {
    asyncResult: true,
  },
  parameters: {
    asyncResult: true,
  },
});
