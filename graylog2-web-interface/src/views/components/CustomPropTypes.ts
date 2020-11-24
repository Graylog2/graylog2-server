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
import PropTypes from 'prop-types';
import ImmutablePropTypes from 'react-immutable-proptypes';
import { get } from 'lodash';

import TFieldType from 'views/logic/fieldtypes/FieldType';
import TFieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import View from 'views/logic/views/View';

const TimeRangeType = PropTypes.oneOf(['relative', 'absolute', 'keyword']);
const FieldType = PropTypes.instanceOf(TFieldType);
const FieldTypeMapping = PropTypes.instanceOf(TFieldTypeMapping);
const FieldListType = ImmutablePropTypes.listOf(FieldTypeMapping);

const CurrentView = PropTypes.exact({
  activeQuery: PropTypes.string.isRequired,
  view: PropTypes.instanceOf(View).isRequired,
  dirty: PropTypes.bool.isRequired,
  isNew: PropTypes.bool.isRequired,
});

export type CurrentViewType = {
  activeQuery: string,
};

const BackendMessage = PropTypes.exact({
  index: PropTypes.string.isRequired,
  message: PropTypes.exact({
    _id: PropTypes.string.isRequired,
  }).isRequired,
});

const Message = PropTypes.exact({
  id: PropTypes.string.isRequired,
  index: PropTypes.string.isRequired,
  fields: PropTypes.object.isRequired,
  formatted_fields: PropTypes.object,
  highlight_ranges: PropTypes.object,
  decoration_stats: PropTypes.exact({
    added_fields: PropTypes.object,
    changed_fields: PropTypes.object,
    removed_fields: PropTypes.object,
  }),
});

const ValidElements = PropTypes.oneOfType([
  PropTypes.element,
  PropTypes.func,
  PropTypes.string,
]);
const OneOrMoreChildren = PropTypes.oneOfType([
  ValidElements,
  PropTypes.arrayOf(ValidElements),
]);

const prototypesOf = (target) => {
  let i = target;
  const result = [];

  while (i) {
    try {
      const prototype = Object.getPrototypeOf(i);

      result.push(prototype);
      i = prototype;
    } catch {
      i = undefined;
    }
  }

  return result;
};

const createInstanceOf = (expectedClass, required = false) => {
  const expectedConstructorName = get(expectedClass, 'name');

  // eslint-disable-next-line consistent-return
  return (props, propName, componentName) => {
    const value = props[propName];

    if (!value) {
      return required
        ? new Error(`Invalid prop ${propName} supplied to ${componentName}: expected to be instance of ${expectedConstructorName} but found ${value} instead`)
        : undefined;
    }

    const valueConstructorName = get(prototypesOf(value)[0], ['constructor', 'name']);
    const constructorNames = prototypesOf(value)
      .map((proto) => get(proto, ['constructor', 'name']))
      .filter((name) => name !== undefined);

    if (!constructorNames.includes(expectedConstructorName)) {
      return new Error(`Invalid prop ${propName} supplied to ${componentName}: ${valueConstructorName} expected to be instance of ${expectedConstructorName}`);
    }
  };
};

const instanceOf = (expected) => Object.assign(
  createInstanceOf(expected, false),
  { isRequired: createInstanceOf(expected, true) },
);

export default ({
  ...PropTypes,
  BackendMessage,
  Message,
  CurrentView,
  FieldListType,
  FieldType,
  OneOrMoreChildren,
  TimeRangeType,
  instanceOf,
});
