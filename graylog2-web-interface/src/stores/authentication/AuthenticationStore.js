import Reflux from 'reflux';

import URLUtils from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';

import UserNotification from 'util/UserNotification';

import ActionsProvider from 'injection/ActionsProvider';

const AuthenticationActions = ActionsProvider.getActions('Authentication');


const AuthenticationStore = Reflux.createStore({
  listenables: [AuthenticationActions],
  sourceUrl: '/system/authentication/config',

  getInitialState() {
    return {
      authenticators: null,
    };
  },

  load() {
    const url = URLUtils.qualifyUrl(this.sourceUrl);
    const promise = fetch('GET', url)
      .then(
        (response) => {
          this.trigger({ authenticators: response });
          return response;
        },
        (error) => UserNotification.error(`Unable to load authentication configuration: ${error}`, 'Could not load authenticators'),
      );

    AuthenticationActions.load.promise(promise);
  },

  update(type, config) {
    const url = URLUtils.qualifyUrl(this.sourceUrl);
    if (type === 'providers') {
      const promise = fetch('PUT', url, config)
        .then(
          (response) => {
            this.trigger({ authenticators: response });
            UserNotification.success('Configuration updated successfully');
            return response;
          },
          (error) => UserNotification.error(`Unable to save authentication provider configuration: ${error}`, 'Could not save configuration'),
        );
      AuthenticationActions.update.promise(promise);
    }
  },
});

export default AuthenticationStore;
