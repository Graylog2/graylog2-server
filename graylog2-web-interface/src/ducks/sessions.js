import { combineReducers } from 'redux';
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

  const sessionId = selectors.getSessionId(getState().sessions);
  return new Builder('DELETE', URLUtils.qualifyUrl(ApiRoutes.SessionsApiController.logout(sessionId).url))
    .authenticated()
    .build()
    .then(
      (response) => {
        dispatch({
          type: actionTypes.LOGOUT_SUCCESS,
          isLoggedOut: response.ok || response.status === 401,
        });
        history.push(Routes.STARTPAGE);
      },
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

  const sessionsState = getState().sessions;
  const sessionId = selectors.getSessionId(sessionsState);
  const username = selectors.getUsername(sessionsState);

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
selectors.getIsLoggedIn = state => state.session.isLoggedIn;
selectors.getSessionId = state => state.session.sessionId;
selectors.getUsername = state => state.session.username;

const isLoadingReducer = (state = false, action) => {
  switch (action.type) {
    case actionTypes.LOGIN_REQUEST:
    case actionTypes.LOGOUT_REQUEST:
      return true;
    case actionTypes.LOGIN_SUCCESS:
    case actionTypes.LOGIN_FAILURE:
    case actionTypes.LOGOUT_SUCCESS:
    case actionTypes.LOGOUT_FAILURE:
      return false;
    default:
      return state;
  }
};

const isValidatingReducer = (state = true, action) => {
  switch (action.type) {
    case actionTypes.VALIDATION_REQUEST:
      return true;
    case actionTypes.VALIDATION_SUCCESS:
    case actionTypes.VALIDATION_FAILURE:
      return false;
    default:
      return state;
  }
};

const errorReducer = (state = null, action) => {
  let errorMessage;
  switch (action.type) {
    case actionTypes.LOGIN_FAILURE:
      if (action.error.additional.status === 401) {
        errorMessage = 'Invalid credentials, please verify them and retry.';
      } else {
        errorMessage = `Error - the server returned: ${action.error.additional.status} - ${action.error.message}`;
      }
      return errorMessage;
    case actionTypes.LOGOUT_FAILURE:
    case actionTypes.VALIDATION_FAILURE:
      return action.error.message;
    case actionTypes.RESET_LOGIN_ERROR:
    case actionTypes.LOGIN_REQUEST:
    case actionTypes.LOGOUT_REQUEST:
    case actionTypes.VALIDATION_REQUEST:
    case actionTypes.LOGIN_SUCCESS:
    case actionTypes.LOGOUT_SUCCESS:
    case actionTypes.VALIDATION_SUCCESS:
      return null;
    default:
      return state;
  }
};

const storeSession = (state, sessionId, username) => {
  return combineState(state, {
    isLoggedIn: true,
    sessionId: sessionId,
    username: username,
  });
};

const clearSession = (state) => {
  return combineState(state, {
    isLoggedIn: false,
    sessionId: undefined,
    username: undefined,
  });
};

const sessionReducer = (state = {
  isLoggedIn: false,
  sessionId: undefined,
  username: undefined,
}, action) => {
  switch (action.type) {
    case actionTypes.LOGIN_SUCCESS:
      return storeSession(state, action.sessionId, action.username);
    case actionTypes.LOGOUT_SUCCESS:
      return action.isLoggedOut ? clearSession(state) : state;
    case actionTypes.LOGOUT_FAILURE:
      return clearSession(state);
    case actionTypes.VALIDATION_SUCCESS:
      if (action.isValid) {
        return storeSession(state, action.sessionId, action.username);
      }
      return clearSession(state);
    case actionTypes.VALIDATION_FAILURE:
      return clearSession(state);
    default:
      return state;
  }
};

const frontendReducers = combineReducers({
  isLoading: isLoadingReducer,
  isValidating: isValidatingReducer,
  error: errorReducer,
});

export default combineReducers({
  frontend: frontendReducers,
  session: sessionReducer,
});

export { actions, selectors };
