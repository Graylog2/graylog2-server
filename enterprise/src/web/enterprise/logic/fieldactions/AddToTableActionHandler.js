import { SelectedFieldsActions } from 'enterprise/stores/SelectedFieldsStore';

export default (viewId, queryId, field) => {
  SelectedFieldsActions.add(queryId, field);
};
