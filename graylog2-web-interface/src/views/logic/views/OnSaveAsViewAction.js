// @flow strict
import UserNotification from 'util/UserNotification';
import Routes from 'routing/Routes';
import { ViewManagementActions } from 'views/stores/ViewManagementStore';
import { ViewActions } from 'views/stores/ViewStore';
import View from './View';

type Router = {
  push: (string) => Promise<void>,
};

export default (view: View, router: Router) => {
  return ViewManagementActions.create(view)
    .then(() => ViewActions.load(view))
    .then((state) => router.push(Routes.dashboard_show(state.view.id)))
    .then(() => UserNotification.success(`Saving view "${view.title}" was successful!`, 'Success!'))
    .catch((error) => UserNotification.error(`Saving view failed: ${error}`, 'Error!'));
};
