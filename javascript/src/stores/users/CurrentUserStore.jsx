import Reflux from 'reflux';

import URLUtils from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';

import SessionActions from 'actions/sessions/SessionActions';
import SessionStore from 'stores/sessions/SessionStore';

const CurrentUserStore = Reflux.createStore({
  listenable: [SessionActions],
  sourceUrl: '/users',
  currentUser: undefined,

  init() {
    this.listenTo(SessionStore, this.update, this.update);
  },

  getInitialState() {
    return {currentUser: this.currentUser};
  },

  update(sessionInfo) {
    if (sessionInfo.sessionId && sessionInfo.username) {
      const username = sessionInfo.username;

      const promise = fetch('GET', URLUtils.qualifyUrl(this.sourceUrl + '/' + username))
        .then((resp) => {
          this.currentUser = resp;
          this.trigger({currentUser: this.currentUser});
        });
    }
  },
});

export default CurrentUserStore;
