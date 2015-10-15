import Reflux from 'reflux';
import SessionActions from 'actions/sessions/SessionActions';
import URLUtils from 'util/URLUtils';
import fetch, { Builder } from 'logic/rest/FetchProvider';

const SessionStore = Reflux.createStore({
  listenables: [SessionActions],
  sourceUrl: '/system/sessions',
  sessionId: undefined,
  username: undefined,

  init() {
    if (localStorage.getItem('sessionId') !== undefined) {
      this.sessionId = localStorage.getItem('sessionId');
    }
    if (localStorage.getItem('username') !== undefined) {
      this.username = localStorage.getItem('username');
    }
    this._propagateState();
  },
  getInitialState() {
    return this.getSessionInfo();
  },

  login(username, password, host) {
    const builder = new Builder('POST', URLUtils.qualifyUrl(this.sourceUrl))
      .json({username: username, password: password, host: host});
    const promise = builder.build()
      .then((sessionInfo) => {
        return { sessionId: sessionInfo.session_id, username: username };
      });

    SessionActions.login.promise(promise);
  },
  logout(sessionId) {
    const promise = new Builder('DELETE', URLUtils.qualifyUrl(this.sourceUrl + '/' + sessionId))
      .authenticated()
      .build()
      .then((resp) => {
        if (resp.ok || resp.status == 401) {
          this._removeSession();
        }
      }, this._removeSession);

    SessionActions.logout.promise(promise);
  },

  _removeSession() {
    delete localStorage.sessionId;
    delete localStorage.username;
    this.sessionId = undefined;
    this.username = undefined;
    this._propagateState();
  },

  _propagateState() {
    this.trigger(this.getSessionInfo());
  },

  loginCompleted(sessionInfo) {
    localStorage.setItem('sessionId', sessionInfo.sessionId);
    localStorage.setItem('username', sessionInfo.username);
    this.sessionId = sessionInfo.sessionId;
    this.username = sessionInfo.username;
    this._propagateState();
  },
  isLoggedIn() {
    return this.sessionId !== undefined && this.sessionId !== null;
  },
  getSessionId() {
    return this.sessionId;
  },
  getSessionInfo() {
    return {sessionId: this.sessionId, username: this.username};
  },
});

export default SessionStore;
