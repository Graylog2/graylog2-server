import URLUtils from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import { Builder } from 'logic/rest/FetchProvider';
import { combineState } from 'ducks/ducksHelpers';
import Routes from 'routing/Routes';
import history from 'util/History';

const actionTypes = {
  LOGIN_REQUEST: 'graylog/sessions/LOGIN_REQUEST',
  LOGIN_SUCCESS: 'graylog/sessions/LOGIN_SUCCESS',
  LOGIN_FAILURE: 'graylog/sessions/LOGIN_FAILURE',
  LOGOUT_REQUEST: 'graylog/sessions/LOGOUT_REQUEST',
  LOGOUT_SUCCESS: 'graylog/sessions/LOGOUT_SUCCESS',
  LOGOUT_FAILURE: 'graylog/sessions/LOGOUT_FAILURE',
  VALIDATION_REQUEST: 'graylog/sessions/VALIDATION_REQUEST',
  VALIDATION_SUCCESS: 'graylog/sessions/VALIDATION_SUCCESS',
  VALIDATION_FAILURE: 'graylog/sessions/VALIDATION_FAILURE',
  RESET_LOGIN_ERROR: 'graylog/sessions/RESET_LOGIN_ERROR',
};
const actions = {};
const selectors = {};

actions.resetLoginError = () => ({
  type: actionTypes.RESET_LOGIN_ERROR,
});

actions.login = (username, password, host) => (dispatch) => {
  dispatch({
    type: actionTypes.LOGIN_REQUEST,
  });

  return new Builder('POST', URLUtils.qualifyUrl(ApiRoutes.SessionsApiController.login().url))
    .json({ username: username, password: password, host: host })
    .build()
    .then(
      response =>
        dispatch({
          type: actionTypes.LOGIN_SUCCESS,
          sessionId: response.session_id,
          username: username,
        }),
      error =>
        dispatch({
          type: actionTypes.LOGIN_FAILURE,
          error: error,
        }),
    );
};

actions.logout = () => (dispatch, getState) => {
  dispatch({
    type: actionTypes.LOGOUT_REQUEST,
  });

  return new Builder('DELETE', URLUtils.qualifyUrl(ApiRoutes.SessionsApiController.logout(getState().sessionId).url))
    .authenticated()
    .build()
    .then(
      response =>
        dispatch({
          type: actionTypes.LOGOUT_SUCCESS,
          isLoggedOut: response.ok || response.status === 401,
        }),
      error =>
        dispatch({
          type: actionTypes.LOGOUT_FAILURE,
          error: error,
        }),
    );
};

actions.validate = () => (dispatch, getState) => {
  dispatch({
    type: actionTypes.VALIDATION_REQUEST,
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
          type: actionTypes.VALIDATION_SUCCESS,
          isValid: response.is_valid,
          sessionId: sessionId || response.session_id,
          username: username || response.username,
        }),
      error => dispatch({
        type: actionTypes.VALIDATION_FAILURE,
        error: error,
      }),
    );
};

selectors.getIsLoading = state => state.frontend.isLoading;
selectors.getIsValidating = state => state.frontend.isValidating;
selectors.getError = state => state.frontend.error;
selectors.getIsLoggedIn = state => state.isLoggedIn;
selectors.getSessionId = state => state.sessionId;
selectors.getUsername = state => state.username;

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
    case actionTypes.LOGIN_REQUEST:
      return combineState(state, { frontend: { isLoading: true } });
    case actionTypes.LOGIN_SUCCESS:
      return storeSession(state, action.sessionId, action.username);
    case actionTypes.LOGIN_FAILURE:
      return loginFailure(state, action.error);
    case actionTypes.LOGOUT_REQUEST:
      return combineState(state, { frontend: { isLoading: true } });
    case actionTypes.LOGOUT_SUCCESS:
      if (action.isLoggedOut) {
        return doLogout(state);
      }
      return combineState(state, { frontend: { isLoading: false } });
    case actionTypes.LOGOUT_FAILURE:
      return doLogout(state, action.error);
    case actionTypes.VALIDATION_REQUEST:
      return combineState(state, { frontend: { isValidating: true } });
    case actionTypes.VALIDATION_SUCCESS:
      if (action.isValid) {
        return storeSession(state, action.sessionId, action.username);
      }
      return clearSession(state);
    case actionTypes.VALIDATION_FAILURE:
      return clearSession(state);
    case actionTypes.RESET_LOGIN_ERROR:
      return combineState(state, { frontend: { error: undefined } });
    default:
      return state;
  }
}

export { actions, selectors };
