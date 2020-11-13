/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
// @flow strict
import { isEqualWith, isFunction } from 'lodash';

const hasFn = (obj, fn) => (obj && obj[fn] && isFunction(obj[fn]));
const hasEquals = (obj) => hasFn(obj, 'equals');
const hasEqualsForSearch = (obj) => hasFn(obj, 'equalsForSearch');
const isImmutable = (obj) => hasFn(obj, 'toJS');

const _isEqual = (first, second) => {
  if (hasEqualsForSearch(first)) {
    return first.equalsForSearch(second);
  }

  if (isImmutable(first) && isImmutable(second)) {
    return isEqualWith(first.toJS(), second.toJS(), _isEqual);
  }

  if (hasEquals(first)) {
    return first.equals(second);
  }

  return undefined;
};

const isEqualForSearch = (first: any, second: any): boolean => isEqualWith(first, second, _isEqual);

export default isEqualForSearch;
