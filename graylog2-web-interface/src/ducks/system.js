import URLUtils from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';
import { combineState } from 'ducks/ducksHelpers';

const SYSTEM_INFO_REQUEST = 'graylog/system/SYSTEM_INFO_REQUEST';
const SYSTEM_INFO_SUCCESS = 'graylog/system/SYSTEM_INFO_SUCCESS';
const SYSTEM_INFO_FAILURE = 'graylog/system/SYSTEM_INFO_FAILURE';

const requestSystemInfo = () => ({
  type: SYSTEM_INFO_REQUEST,
});

const receiveSystemInfo = response => ({
  type: SYSTEM_INFO_SUCCESS,
  response: response,
});

const failedSystemInfo = error => ({
  type: SYSTEM_INFO_FAILURE,
  error: error,
});

export const loadSystemInfo = () => (dispatch) => {
  dispatch(requestSystemInfo());
  const url = URLUtils.qualifyUrl(ApiRoutes.SystemApiController.info().url);
  return fetch('GET', url)
    .then(
      response => dispatch(receiveSystemInfo(response)),
      error => dispatch(failedSystemInfo(error)),
    );
};

const initialState = {
  frontend: {
    isLoading: true,
    error: undefined,
  },
  entities: {
    systemInfo: undefined,
  },
};

export default function reducer(state = initialState, action) {
  switch (action.type) {
    case SYSTEM_INFO_REQUEST:
      return combineState(state, { frontend: { isLoading: true } });
    case SYSTEM_INFO_SUCCESS:
      return combineState(state, {
        frontend: { isLoading: false },
        entities: { systemInfo: action.response },
      });
    case SYSTEM_INFO_FAILURE:
      return combineState(state, {
        frontend: { isLoading: false, error: action.error.message },
      });
    default:
      return state;
  }
}
