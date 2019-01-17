import React from 'react';
import PropTypes from 'prop-types';

import { isString, trim, truncate as trunc } from 'lodash';

import UserTimezoneTimestamp from 'enterprise/components/common/UserTimezoneTimestamp';
import FieldType from 'enterprise/logic/fieldtypes/FieldType';

import EmptyValue from './EmptyValue';
import CustomPropTypes from './CustomPropTypes';

const _formatValue = (value, truncate) => {
  const stringified = isString(value) ? value : JSON.stringify(value);
  return trim(stringified) === '' ? <EmptyValue /> : (truncate ? trunc(stringified) : stringified);
};

const _renderTypeSpecific = (value, { type }, truncate) => {
  if (value === undefined) {
    return null;
  }
  switch (type) {
    case 'date': return <UserTimezoneTimestamp dateTime={value} />;
    case 'boolean': return String(value);
    default: return _formatValue(value, truncate);
  }
};

const TypeSpecificValue = ({ value, type, truncate = false }) => (
  <React.Fragment>
    {_renderTypeSpecific(value, type, truncate)}
  </React.Fragment>
);

TypeSpecificValue.propTypes = {
  truncate: PropTypes.bool,
  type: CustomPropTypes.FieldType,
  value: PropTypes.any.isRequired,
};

TypeSpecificValue.defaultProps = {
  truncate: false,
  type: FieldType.Unknown,
};

export default TypeSpecificValue;
