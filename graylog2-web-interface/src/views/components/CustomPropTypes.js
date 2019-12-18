// @flow strict
import PropTypes from 'prop-types';
import ImmutablePropTypes from 'react-immutable-proptypes';
import { get } from 'lodash';

import TFieldType from 'views/logic/fieldtypes/FieldType';
import TFieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';

const TimeRangeType = PropTypes.oneOf(['relative', 'absolute', 'keyword']);
const FieldType = PropTypes.instanceOf(TFieldType);
const FieldTypeMapping = PropTypes.instanceOf(TFieldTypeMapping);
const FieldListType = ImmutablePropTypes.listOf(FieldTypeMapping);

const CurrentView = PropTypes.shape({
  activeQuery: PropTypes.string,
});

export type CurrentViewType = {
  activeQuery: string,
};

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
      .map(proto => get(proto, ['constructor', 'name']))
      .filter(name => name !== undefined);
    if (!constructorNames.includes(expectedConstructorName)) {
      return new Error(`Invalid prop ${propName} supplied to ${componentName}: ${valueConstructorName} expected to be instance of ${expectedConstructorName}`);
    }
  };
};

const instanceOf = expected => Object.assign(
  createInstanceOf(expected, false),
  { isRequired: createInstanceOf(expected, true) },
);

export default Object.assign({}, PropTypes, { CurrentView, FieldListType, FieldType, OneOrMoreChildren, TimeRangeType, instanceOf });
