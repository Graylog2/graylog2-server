import Reflux from 'reflux';
import SessionActions from 'actions/sessions/SessionActions';
import URLUtils from 'util/URLUtils';
import fetch, { Builder } from 'logic/rest/FetchProvider';

const SessionStore = Reflux.createStore({
  listenables: [SessionActions],
  sourceUrl: '/system/sessions',
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
    const builder = new Builder('POST', URLUtils.qualifyUrl(this.sourceUrl))
      .json({username: username, password: password, host: host});
    const promise = builder.build()
      .then((sessionInfo) => {
        return sessionInfo.session_id;
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
    this.sessionId = undefined;
    this.trigger({sessionId: this.sessionId});
  },

  loginCompleted(sessionId) {
    localStorage.setItem('sessionId', sessionId);
    this.sessionId = sessionId;
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
