import { action, observable } from 'mobx';

import Store from 'logic/local-storage/Store';
import URLUtils from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import { Builder } from 'logic/rest/FetchProvider';

const sourceUrl = '/system/sessions';

class SessionStore {
  constructor() {
    this.state = observable.shallowObject({
      sessionId: undefined,
      username: undefined,
      error: undefined,
      isLoading: false,
      isValidatingSession: true,
    }, 'SessionStore state');
    this.validate();
  }

  get isLoading() {
    return this.state.isLoading;
  }

  get isValidatingSession() {
    return this.state.isValidatingSession;
  }

  get error() {
    return this.state.error;
  }

  get sessionId() {
    return this.state.sessionId;
  }

  get username() {
    return this.state.username;
  }

  get isLoggedIn() {
    return this.state.sessionId !== undefined && this.state.sessionId !== null;
  }

  login = action(function login(username, password, host) {
    this.state.isLoading = true;
    this.state.error = undefined;
    const builder = new Builder('POST', URLUtils.qualifyUrl(sourceUrl))
      .json({ username: username, password: password, host: host });
    return builder.build()
      .then(
        (sessionInfo) => {
          this.persistSession({
            sessionId: sessionInfo.session_id,
            username: username,
          });
          return sessionInfo;
        },
        (error) => {
          if (error.additional.status === 401) {
            this.state.error = 'Invalid credentials, please verify them and retry.';
          } else {
            this.state.error = `Error - the server returned: ${error.additional.status} - ${error.message}`;
          }
        },
      )
      .finally(action(() => {
        this.state.isLoading = false;
      }));
  })

  logout = action(function logout(sessionId) {
    return new Builder('DELETE', URLUtils.qualifyUrl(`${sourceUrl}/${sessionId}`))
      .authenticated()
      .build()
      .then((response) => {
        if (response.ok || response.status === 401) {
          this.removeSession();
        }
        return response;
      }, this.removeSession);
  })

  validate = action(function validate() {
    const sessionId = Store.get('sessionId');
    const username = Store.get('username');
    this.state.isValidatingSession = true;
    this.validateSession(sessionId)
      .then((response) => {
        if (response.is_valid) {
          this.persistSession({
            sessionId: sessionId || response.session_id,
            username: username || response.username,
          });
          return response;
        }
        if (sessionId && username) {
          this.removeSession();
        }

        return response;
      })
      .finally(action(() => {
        this.state.isValidatingSession = false;
      }));
  })

  validateSession(sessionId) {
    return new Builder('GET', URLUtils.qualifyUrl(ApiRoutes.SessionsApiController.validate().url))
      .session(sessionId)
      .json()
      .build();
  }

  removeSession = action(function removeSession() {
    Store.delete('sessionId');
    Store.delete('username');
    this.state.sessionId = undefined;
    this.state.username = undefined;
  })

  persistSession = action(function persistSession(sessionInfo) {
    Store.set('sessionId', sessionInfo.sessionId);
    Store.set('username', sessionInfo.username);
    this.state.sessionId = sessionInfo.sessionId;
    this.state.username = sessionInfo.username;
  })
}

export default new SessionStore();
