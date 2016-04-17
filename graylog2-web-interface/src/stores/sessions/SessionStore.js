import Reflux from 'reflux';

import Store from 'logic/local-storage/Store';
import URLUtils from 'util/URLUtils';
import { Builder } from 'logic/rest/FetchProvider';

import ActionsProvider from 'injection/ActionsProvider';
const SessionActions = ActionsProvider.getActions('Session');

const SessionStore = Reflux.createStore({
  listenables: [SessionActions],
  sourceUrl: '/system/sessions',
  sessionId: undefined,
  username: undefined,

  init() {
    this.sessionId = Store.get('sessionId');
    this.username = Store.get('username');
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
        if (resp.ok || resp.status === 401) {
          this._removeSession();
        }
      }, this._removeSession);

    SessionActions.logout.promise(promise);
  },

  _removeSession() {
    Store.delete('sessionId');
    Store.delete('username');
    this.sessionId = undefined;
    this.username = undefined;
    this._propagateState();
  },

  _propagateState() {
    this.trigger(this.getSessionInfo());
  },

  loginCompleted(sessionInfo) {
    Store.set('sessionId', sessionInfo.sessionId);
    Store.set('username', sessionInfo.username);
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
