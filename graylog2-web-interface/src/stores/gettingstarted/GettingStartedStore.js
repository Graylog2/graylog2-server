import Reflux from 'reflux';

import fetch from 'logic/rest/FetchProvider';

import ActionsProvider from 'injection/ActionsProvider';
const GettingStartedActions = ActionsProvider.getActions('GettingStarted');

import UserNotification from 'util/UserNotification';
import URLUtils from 'util/URLUtils';

const GettingStartedStore = Reflux.createStore({
  listenables: [GettingStartedActions],
  sourceUrl: '/system/gettingstarted',
  status: undefined,

  init() {
    this.getStatus();
  },

  getInitialState() {
    return {status: this.status};
  },

  get() {
    return this.status;
  },

  getStatus() {
    const promise = fetch('GET', URLUtils.qualifyUrl(this.sourceUrl));
    promise
      .then(response => {
        this.status = response;
        this.trigger({status: this.status});
      })
      .catch(console.error);

    GettingStartedActions.getStatus.promise(promise);
  },

  dismiss() {
    const promise = fetch('POST', URLUtils.qualifyUrl(`${this.sourceUrl}/dismiss`), '{}');
    promise
      .then(() => this.getStatus())
      .catch((error) => {
        UserNotification.error('Dismissing Getting Started Guide failed with status: ' + error,
          'Could not dismiss guide');
      });

    GettingStartedActions.dismiss.promise(promise);
  },
});

export default GettingStartedStore;
