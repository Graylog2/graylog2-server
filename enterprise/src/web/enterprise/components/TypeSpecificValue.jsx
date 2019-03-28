// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';

import { isString, trim, truncate as trunc } from 'lodash';

import UserTimezoneTimestamp from 'enterprise/components/common/UserTimezoneTimestamp';
import FieldType from 'enterprise/logic/fieldtypes/FieldType';

import EmptyValue from './EmptyValue';
import CustomPropTypes from './CustomPropTypes';

const _formatValue = (value, truncate, render) => {
  const stringified = isString(value) ? value : JSON.stringify(value);
  const Component = render;
  return trim(stringified) === '' ? <EmptyValue /> : <Component value={(truncate ? trunc(stringified) : stringified)} />;
};

type RenderProps = {
  value: any,
};

type Props = {
  value: any,
  type: FieldType,
  truncate?: boolean,
  render?: React.ComponentType<RenderProps>,
};

const defaultComponent = ({ value }: RenderProps) => value;

const TypeSpecificValue = ({ value, render = defaultComponent, type = FieldType.Unknown, truncate = false }: Props) => {
  if (value === undefined) {
    return null;
  }
  switch (type.type) {
    case 'date': return <UserTimezoneTimestamp dateTime={value} />;
    case 'boolean': return String(value);
    default: return _formatValue(value, truncate, render);
  }
};

TypeSpecificValue.propTypes = {
  truncate: PropTypes.bool,
  type: CustomPropTypes.FieldType,
  value: PropTypes.any.isRequired,
};

TypeSpecificValue.defaultProps = {
  render: defaultComponent,
};

export default TypeSpecificValue;
