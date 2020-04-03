import UserNotification from 'util/UserNotification';
import { ViewManagementActions } from 'views/stores/ViewManagementStore';
import { ViewActions } from 'views/stores/ViewStore';

export default (newView) => {
  return ViewActions.update(newView).then(({ view }) => {
    return ViewManagementActions.update(view);
  }).then(({ title }) => {
    return UserNotification.success(`Saving view "${title}" was successful!`, 'Success!');
  }).catch((error) => UserNotification.error(`Saving view failed: ${error}`, 'Error!'));
};
