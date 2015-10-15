import Reflux from 'reflux';
import SessionActions from 'actions/sessions/SessionActions';
import URLUtils from 'util/URLUtils';
import fetch, { Builder } from 'logic/rest/FetchProvider';
import SessionStore from 'stores/sessions/SessionStore';

const CurrentUserStore = Reflux.createStore({
  listenable: [SessionActions],
  sourceUrl: '/users',

  init() {
    this.listenTo(SessionStore, this.update);
    if (SessionStore.getSessionId()) {
      this.update(SessionStore.getSessionInfo());
    }
  },

  update(sessionInfo) {
    if (sessionInfo.sessionId && sessionInfo.username) {
      const username = sessionInfo.username;

      const promise = fetch('GET', URLUtils.qualifyUrl(this.sourceUrl + '/' + username))
        .then((resp) => {
          this.trigger({currentUser: resp});
        });
    }
  }
});
export default CurrentUserStore;
