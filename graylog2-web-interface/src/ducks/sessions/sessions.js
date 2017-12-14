import URLUtils from 'util/URLUtils';
import Store from 'logic/local-storage/Store';
import ApiRoutes from 'routing/ApiRoutes';
import { Builder } from 'logic/rest/FetchProvider';
import { combineState } from 'ducks/ducksHelpers';
import Routes from 'routing/Routes';
import history from 'util/History';

export const LOGIN_REQUEST = 'graylog/sessions/LOGIN_REQUEST';
export const LOGIN_SUCCESS = 'graylog/sessions/LOGIN_SUCCESS';
export const LOGIN_FAILURE = 'graylog/sessions/LOGIN_FAILURE';

export const LOGOUT_REQUEST = 'graylog/sessions/LOGOUT_REQUEST';
export const LOGOUT_SUCCESS = 'graylog/sessions/LOGOUT_SUCCESS';
export const LOGOUT_FAILURE = 'graylog/sessions/LOGOUT_FAILURE';

const requestLogin = () => ({
  type: LOGIN_REQUEST,
});

const receiveLogin = (sessionId, username) => ({
  type: LOGIN_SUCCESS,
  sessionId: sessionId,
  username: username,
});

const failedLogin = error => ({
  type: LOGIN_FAILURE,
  error: error,
});

const requestLogout = () => ({
  type: LOGOUT_REQUEST,
});

const receiveLogout = isLoggedOut => ({
  type: LOGOUT_SUCCESS,
  isLoggedOut: isLoggedOut,
});

const failedLogout = error => ({
  type: LOGOUT_FAILURE,
  error: error,
});

export const login = (username, password, host) => (dispatch) => {
  dispatch(requestLogin());

  return new Builder('POST', URLUtils.qualifyUrl(ApiRoutes.SessionsApiController.login().url))
    .json({ username: username, password: password, host: host })
    .build()
    .then(
      response => dispatch(receiveLogin(response.session_id, username)),
      error => dispatch(failedLogin(error)),
    );
};

export const logout = () => (dispatch, getState) => {
  dispatch(requestLogout());

  return new Builder('DELETE', URLUtils.qualifyUrl(ApiRoutes.SessionsApiController.logout(getState().sessionId).url))
    .authenticated()
    .build()
    .then(
      response => dispatch(receiveLogout(response.ok || response.status === 401)),
      error => dispatch(failedLogout(error)),
    );
};

const loginSuccessful = (state, sessionId, username) => {
  Store.set('sessionId', sessionId);
  Store.set('username', username);

  return combineState(state, {
    frontend: { isLoading: false },
    loggedIn: true,
    sessionId: sessionId,
    username: username,
  });
};

const loginFailure = (state, error) => {
  let errorMessage;
  if (error) {
    if (error.additional.status === 401) {
      errorMessage = 'Invalid credentials, please verify them and retry.';
    } else {
      errorMessage = `Error - the server returned: ${error.additional.status} - ${error.message}`;
    }
  }
  return combineState(state, {
    frontend: { isLoading: false, error: errorMessage },
  });
};

const logoutSuccessful = (state, error) => {
  Store.delete('sessionId');
  Store.delete('username');

  history.push(Routes.STARTPAGE);

  return combineState(state, {
    frontend: { isLoading: false },
    loggedIn: false,
    sessionId: undefined,
    username: undefined,
    error: error,
  });
};

const initialState = {
  frontend: {
    isLoading: false,
    error: undefined,
  },
  loggedIn: false,
  sessionId: undefined,
  username: undefined,
};

export default function reducer(state = initialState, action) {
  switch (action.type) {
    case LOGIN_REQUEST:
      return combineState(state, { frontend: { isLoading: true, error: undefined } });
    case LOGIN_SUCCESS:
      return loginSuccessful(state, action.sessionId, action.username);
    case LOGIN_FAILURE:
      return loginFailure(state, action.error);
    case LOGOUT_REQUEST:
      return combineState(state, { frontend: { isLoading: true, error: undefined } });
    case LOGOUT_SUCCESS:
      if (action.isLoggedOut) {
        return logoutSuccessful(state);
      }
      return state;
    case LOGOUT_FAILURE:
      return logoutSuccessful(state, action.error);
    default:
      return state;
  }
}
