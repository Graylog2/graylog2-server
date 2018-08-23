import { SelectedFieldsActions } from 'enterprise/stores/SelectedFieldsStore';

export default (queryId, field) => {
  SelectedFieldsActions.add(field);
};
