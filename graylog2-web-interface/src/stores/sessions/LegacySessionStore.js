import Reflux from 'reflux';

import Store from 'logic/local-storage/Store';
import URLUtils from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import { Builder } from 'logic/rest/FetchProvider';

import ActionsProvider from 'injection/ActionsProvider';
const SessionActions = ActionsProvider.getActions('Session');

const SessionStore = Reflux.createStore({
  listenables: [SessionActions],
  sourceUrl: '/system/sessions',
  sessionId: undefined,
  username: undefined,
  validatingSession: false,

  init() {
    this.validate();
  },
  getInitialState() {
    return this.getSessionInfo();
  },

  validate() {
    const sessionId = Store.get('sessionId');
    const username = Store.get('username');
    this.validatingSession = true;
    this._propagateState();
    this._validateSession(sessionId)
      .then((response) => {
        if (response.is_valid) {
          return SessionActions.login.completed({
            sessionId: sessionId || response.session_id,
            username: username || response.username,
          });
        }

        return response;
      })
      .finally(() => {
        this.validatingSession = false;
        this._propagateState();
      });
  },
  _validateSession(sessionId) {
    return new Builder('GET', URLUtils.qualifyUrl(ApiRoutes.SessionsApiController.validate().url))
      .session(sessionId)
      .json()
      .build();
  },

  _propagateState() {
    this.trigger(this.getSessionInfo());
  },

  isLoggedIn() {
    return this.sessionId !== undefined && this.sessionId !== null;
  },
  loginCompleted(sessionInfo) {
    this.sessionId = sessionInfo.sessionId;
    this.username = sessionInfo.username;
    this._propagateState();
  },
  getSessionId() {
    return this.sessionId;
  },
  getSessionInfo() {
    return { sessionId: this.sessionId, username: this.username, validatingSession: this.validatingSession };
  },
});

export default SessionStore;
