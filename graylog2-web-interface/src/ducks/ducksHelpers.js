import lodash from 'lodash';

export const combineState = (oldState, update) => lodash.merge({}, oldState, update);
