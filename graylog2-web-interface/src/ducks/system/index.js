import { combineReducers } from 'redux';
import jvmReducer from './jvm';
import systemReducer from './system';

export { loadJvmInfo } from './jvm';
export { loadSystemInfo } from './system';
export default combineReducers({ system: systemReducer, jvm: jvmReducer });
