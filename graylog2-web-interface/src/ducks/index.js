import { combineReducers } from 'redux';
import sessionsReducer from './sessions';
import systemReducer from './system/index';

export default combineReducers({ sessions: sessionsReducer, system: systemReducer });
