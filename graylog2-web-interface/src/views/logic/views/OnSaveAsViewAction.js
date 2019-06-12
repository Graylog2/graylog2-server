import UserNotification from 'util/UserNotification';
import Routes from 'routing/Routes';
import { ViewManagementActions } from 'views/stores/ViewManagementStore';
import { ViewActions } from 'views/stores/ViewStore';

export default (view, router) => {
  return ViewManagementActions.create(view)
    .then(() => ViewActions.load(view))
    .then(state => router.push(Routes.pluginRoute('VIEWS_VIEWID')(state.view.id)))
    .then(() => UserNotification.success(`Saving view "${view.title}" was successful!`, 'Success!'))
    .catch(error => UserNotification.error(`Saving view failed: ${error}`, 'Error!'));
};
