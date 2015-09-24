import Reflux from 'reflux';
import SessionActions from 'actions/sessions/SessionActions';
import { fetch, fetchUnauthenticated } from 'logic/rest/FetchProvider';

const SessionStore = Reflux.createStore({
  listenables: [SessionActions],
  sourceUrl: 'http://localhost:12900/system/sessions',
  sessionId: undefined,

  init() {
    if (localStorage.getItem('sessionId') !== undefined) {
      this.sessionId = localStorage.getItem('sessionId');
      this.trigger({sessionId: this.sessionId});
    }
  },
  getInitialState() {
    return { sessionId: this.sessionId };
  },

  login(username, password, host) {
    const promise = fetchUnauthenticated(this.sourceUrl, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      },
      body: JSON.stringify({
        username: username,
        password: password,
        host: host,
      }),
    })
      .then((response) => { return response.json(); })
      .catch((error) => {
        console.log(error);
      })
      .then((sessionInfo) => {
        return sessionInfo.session_id;
      });

    SessionActions.login.promise(promise);
  },
  logout(sessionId) {
    const promise = fetch(this.sourceUrl + '/' + sessionId, {
      method: 'DELETE',
    });

    SessionActions.logout.promise(promise);
  },

  loginCompleted(sessionId) {
    localStorage.setItem('sessionId', sessionId);
    this.sessionId = sessionId;
    this.trigger({sessionId: this.sessionId});
  },
  logoutCompleted() {
    delete localStorage.sessionId;
    this.sessionId = undefined;
    this.trigger({sessionId: this.sessionId});
  },
  isLoggedIn() {
    return this.sessionId !== undefined && this.sessionId !== null;
  },
  getSessionId() {
    return this.sessionId;
  },
});

export default SessionStore;
