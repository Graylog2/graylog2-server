// @flow strict
import { isEqualWith, isFunction } from 'lodash';

const hasFn = (obj, fn) => (obj && obj[fn] && isFunction(obj[fn]));
const hasEquals = (obj) => hasFn(obj, 'equals');
const isImmutable = (obj) => hasFn(obj, 'toJS');

const _isEqual = (first, second) => {
  if (isImmutable(first) && isImmutable(second)) {
    return isEqualWith(first.toJS(), second.toJS(), _isEqual);
  }
  if (hasEquals(first)) {
    return first.equals(second);
  }
  return undefined;
};

const isDeepEqual = (first: any, second: any): boolean => isEqualWith(first, second, _isEqual);

export default isDeepEqual;
