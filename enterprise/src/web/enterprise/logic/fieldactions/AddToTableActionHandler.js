import SelectedFieldsActions from 'enterprise/actions/SelectedFieldsActions';
import SelectedFieldsStore from 'enterprise/stores/SelectedFieldsStore';

export default (viewId, queryId, field) => {
  SelectedFieldsActions.add(queryId, field);
};
