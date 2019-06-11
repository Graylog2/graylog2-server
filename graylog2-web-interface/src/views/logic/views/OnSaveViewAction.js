import UserNotification from 'util/UserNotification';
import { ViewManagementActions } from 'enterprise/stores/ViewManagementStore';

export default (view) => {
  return ViewManagementActions.update(view)
    .then(() => UserNotification.success(`Saving view "${view.title}" was successful!`, 'Success!'))
    .catch(error => UserNotification.error(`Saving view failed: ${error}`, 'Error!'));
};
