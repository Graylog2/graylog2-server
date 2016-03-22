import Reflux from 'reflux';

import URLUtils from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';

import StoreProvider from 'injection/StoreProvider';
const SessionStore = StoreProvider.getStore('Session');
const StartpageStore = StoreProvider.getStore('Startpage');

import ActionsProvider from 'injection/ActionsProvider';
const SessionActions = ActionsProvider.getActions('Session');

const CurrentUserStore = Reflux.createStore({
  listenables: [SessionActions],
  sourceUrl: '/users',
  currentUser: undefined,

  init() {
    this.listenTo(SessionStore, this.sessionUpdate, this.sessionUpdate);
    this.listenTo(StartpageStore, this.reload, this.reload);
  },

  getInitialState() {
    return {currentUser: this.currentUser};
  },

  get() {
    return this.currentUser;
  },

  sessionUpdate(sessionInfo) {
    if (sessionInfo.sessionId && sessionInfo.username) {
      const username = sessionInfo.username;
      this.update(username);
    }
  },

  reload() {
    if (this.currentUser !== undefined) {
      this.update(this.currentUser.username);
    }
  },

  update(username) {
    fetch('GET', URLUtils.qualifyUrl(this.sourceUrl + '/' + username))
      .then((resp) => {
        this.currentUser = resp;
        this.trigger({currentUser: this.currentUser});
      });
  },
});

export default CurrentUserStore;
