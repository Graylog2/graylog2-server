// @flow strict
import UserNotification from 'util/UserNotification';
import { ViewManagementActions } from 'views/stores/ViewManagementStore';
import { ViewActions } from 'views/stores/ViewStore';

import View from './View';
import { loadDashboard } from './Actions';

export default (view: View) => {
  return ViewManagementActions.create(view)
    .then(() => ViewActions.load(view))
    .then((state) => loadDashboard(state.view.id))
    .then(() => UserNotification.success(`Saving view "${view.title}" was successful!`, 'Success!'))
    .catch((error) => UserNotification.error(`Saving view failed: ${error}`, 'Error!'));
};
