import Reflux from 'reflux';

const FilterPreviewActions = Reflux.createActions({
  create: { asyncResult: true },
  execute: { asyncResult: true },
  search: { asyncResult: true },
});

export default FilterPreviewActions;
