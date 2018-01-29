import { action, autorun, observable } from 'mobx';

import URLUtils from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import ApiRoutes from 'routing/ApiRoutes';

import CombinedProvider from 'injection/CombinedProvider';

const { SessionStore } = CombinedProvider.get('Session');
const { StartpageStore } = CombinedProvider.get('Startpage');

class CurrentUserStore {
  constructor() {
    this.state = observable.shallowObject({
      isLoading: false,
      currentUser: undefined,
    }, 'CurrentUserStore state');
  }

  get isLoading() {
    return this.state.isLoading;
  }

  get currentUser() {
    return this.state.currentUser;
  }

  sessionUpdate = action(function sessionUpdate(sessionInfo) {
    if (sessionInfo.sessionId && sessionInfo.username) {
      const username = sessionInfo.username;
      this.update(username);
    } else {
      this.state.currentUser = undefined;
    }
  })

  reload = action(function reload() {
    if (this.state.currentUser !== undefined) {
      return this.update(this.state.currentUser.username);
    }

    return null;
  })

  update = action(function update(username) {
    this.state.isLoading = true;
    return fetch('GET', URLUtils.qualifyUrl(ApiRoutes.UsersApiController.load(encodeURIComponent(username)).url))
      .then(action((response) => {
        this.state.currentUser = response;
        return response;
      }))
      .finally(action(() => {
        this.state.isLoading = false;
      }));
  })
}

const currentUserStore = new CurrentUserStore();

autorun('Update current user', () => {
  currentUserStore.sessionUpdate({
    sessionId: SessionStore.sessionId,
    username: SessionStore.username,
  });
});

// Reloads user after their start page changes, allowing us to redirect them to the right page.
StartpageStore.listen(() => {
  this.reload();
});

export default currentUserStore;
