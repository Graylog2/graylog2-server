import Reflux from 'reflux';
import { reaction } from 'mobx';

import URLUtils from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import ApiRoutes from 'routing/ApiRoutes';

import CombinedProvider from 'injection/CombinedProvider';

const { SessionStore } = CombinedProvider.get('Session');
const { StartpageStore } = CombinedProvider.get('Startpage');

const CurrentUserStore = Reflux.createStore({
  currentUser: undefined,

  init() {
    reaction(
      () => ({
        sessionId: SessionStore.sessionId,
        username: SessionStore.username,
      }),
      (sessionInfo) => {
        this.sessionUpdate(sessionInfo);
      },
    );
    this.listenTo(StartpageStore, this.reload, this.reload);
  },

  getInitialState() {
    return { currentUser: this.currentUser };
  },

  get() {
    return this.currentUser;
  },

  sessionUpdate(sessionInfo) {
    if (sessionInfo.sessionId && sessionInfo.username) {
      const username = sessionInfo.username;
      this.update(username);
    } else {
      this.currentUser = undefined;
      this.trigger({ currentUser: this.currentUser });
    }
  },

  reload() {
    if (this.currentUser !== undefined) {
      return this.update(this.currentUser.username);
    }
  },

  update(username) {
    return fetch('GET', URLUtils.qualifyUrl(ApiRoutes.UsersApiController.load(encodeURIComponent(username)).url))
      .then((resp) => {
        this.currentUser = resp;
        this.trigger({ currentUser: this.currentUser });
      });
  },
});

export default CurrentUserStore;
