import URLUtils from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import { Builder } from 'logic/rest/FetchProvider';
import { combineState } from 'ducks/ducksHelpers';
import Routes from 'routing/Routes';
import history from 'util/History';

const LOGIN_REQUEST = 'graylog/sessions/LOGIN_REQUEST';
const LOGIN_SUCCESS = 'graylog/sessions/LOGIN_SUCCESS';
const LOGIN_FAILURE = 'graylog/sessions/LOGIN_FAILURE';

const LOGOUT_REQUEST = 'graylog/sessions/LOGOUT_REQUEST';
const LOGOUT_SUCCESS = 'graylog/sessions/LOGOUT_SUCCESS';
const LOGOUT_FAILURE = 'graylog/sessions/LOGOUT_FAILURE';

const VALIDATION_REQUEST = 'graylog/sessions/VALIDATION_REQUEST';
const VALIDATION_SUCCESS = 'graylog/sessions/VALIDATION_SUCCESS';
const VALIDATION_FAILURE = 'graylog/sessions/VALIDATION_FAILURE';

const RESET_LOGIN_ERROR = 'graylog/sessions/RESET_LOGIN_ERROR';

export const resetLoginError = () => ({
  type: RESET_LOGIN_ERROR,
});

export const login = (username, password, host) => (dispatch) => {
  dispatch({
    type: LOGIN_REQUEST,
  });

  return new Builder('POST', URLUtils.qualifyUrl(ApiRoutes.SessionsApiController.login().url))
    .json({ username: username, password: password, host: host })
    .build()
    .then(
      response =>
        dispatch({
          type: LOGIN_SUCCESS,
          sessionId: response.session_id,
          username: username,
        }),
      error =>
        dispatch({
          type: LOGIN_FAILURE,
          error: error,
        }),
    );
};

export const logout = () => (dispatch, getState) => {
  dispatch({
    type: LOGOUT_REQUEST,
  });

  return new Builder('DELETE', URLUtils.qualifyUrl(ApiRoutes.SessionsApiController.logout(getState().sessionId).url))
    .authenticated()
    .build()
    .then(
      response =>
        dispatch({
          type: LOGOUT_SUCCESS,
          isLoggedOut: response.ok || response.status === 401,
        }),
      error =>
        dispatch({
          type: LOGOUT_FAILURE,
          error: error,
        }),
    );
};

export const validate = () => (dispatch, getState) => {
  dispatch({
    type: VALIDATION_REQUEST,
  });

  const sessionsStore = getState().sessions;
  const sessionId = sessionsStore.sessionId;
  const username = sessionsStore.username;

  return new Builder('GET', URLUtils.qualifyUrl(ApiRoutes.SessionsApiController.validate().url))
    .session(sessionId)
    .json()
    .build()
    .then(
      response =>
        dispatch({
          type: VALIDATION_SUCCESS,
          isValid: response.is_valid,
          sessionId: sessionId || response.session_id,
          username: username || response.username,
        }),
      error => dispatch({
        type: VALIDATION_FAILURE,
        error: error,
      }),
    );
};

const storeSession = (state, sessionId, username) => {
  return combineState(state, {
    frontend: { isLoading: false, isValidating: false },
    isLoggedIn: true,
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

const clearSession = (state, error) => {
  return combineState(state, {
    frontend: { isLoading: false, isValidating: false, error: error },
    isLoggedIn: false,
    sessionId: undefined,
    username: undefined,
  });
};

const doLogout = (state, error) => {
  history.push(Routes.STARTPAGE);

  return clearSession(state, error);
};

const initialState = {
  frontend: {
    isLoading: false,
    isValidating: true,
    error: undefined,
  },
  isLoggedIn: false,
  sessionId: undefined,
  username: undefined,
};

export default function reducer(state = initialState, action) {
  switch (action.type) {
    case LOGIN_REQUEST:
      return combineState(state, { frontend: { isLoading: true } });
    case LOGIN_SUCCESS:
      return storeSession(state, action.sessionId, action.username);
    case LOGIN_FAILURE:
      return loginFailure(state, action.error);
    case LOGOUT_REQUEST:
      return combineState(state, { frontend: { isLoading: true } });
    case LOGOUT_SUCCESS:
      if (action.isLoggedOut) {
        return doLogout(state);
      }
      return combineState(state, { frontend: { isLoading: false } });
    case LOGOUT_FAILURE:
      return doLogout(state, action.error);
    case VALIDATION_REQUEST:
      return combineState(state, { frontend: { isValidating: true } });
    case VALIDATION_SUCCESS:
      if (action.isValid) {
        return storeSession(state, action.sessionId, action.username);
      }
      return clearSession(state);
    case VALIDATION_FAILURE:
      return clearSession(state);
    case RESET_LOGIN_ERROR:
      return combineState(state, { frontend: { error: undefined } });
    default:
      return state;
  }
}
