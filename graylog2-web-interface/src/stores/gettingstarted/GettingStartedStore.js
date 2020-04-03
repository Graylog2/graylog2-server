import Reflux from 'reflux';

import fetch from 'logic/rest/FetchProvider';

import ActionsProvider from 'injection/ActionsProvider';

import UserNotification from 'util/UserNotification';
import URLUtils from 'util/URLUtils';

const GettingStartedActions = ActionsProvider.getActions('GettingStarted');

const GettingStartedStore = Reflux.createStore({
  listenables: [GettingStartedActions],
  sourceUrl: '/system/gettingstarted',
  status: undefined,

  init() {
    this.getStatus();
  },

  getInitialState() {
    return { status: this.status };
  },

  get() {
    return this.status;
  },

  getStatus() {
    const promise = fetch('GET', URLUtils.qualifyUrl(this.sourceUrl));
    promise
      .then(
        (response) => {
          this.status = response;
          this.trigger({ status: this.status });
          return response;
        },
        (error) => console.error(error),
      );

    GettingStartedActions.getStatus.promise(promise);
  },

  dismiss() {
    const promise = fetch('POST', URLUtils.qualifyUrl(`${this.sourceUrl}/dismiss`), '{}');
    promise
      .then(
        (response) => {
          this.getStatus();
          return response;
        },
        (error) => {
          UserNotification.error(`Dismissing Getting Started Guide failed with status: ${error}`,
            'Could not dismiss guide');
        },
      );

    GettingStartedActions.dismiss.promise(promise);
  },
});

export default GettingStartedStore;
