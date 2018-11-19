import React from 'react';
import PropTypes from 'prop-types';
import { truncate } from 'lodash';

import UserTimezoneTimestamp from 'enterprise/components/common/UserTimezoneTimestamp';
import FieldType from 'enterprise/logic/fieldtypes/FieldType';

import CustomPropTypes from './CustomPropTypes';
import ValueActions from './ValueActions';

const _renderTypeSpecific = (value, { type }) => {
  switch (type) {
    case 'date': return <UserTimezoneTimestamp dateTime={value} />;
    case 'boolean': return String(value);
    default: return value;
  }
};

const Value = ({ children, field, value, queryId, type }) => {
  const element = children || _renderTypeSpecific(value, type);

  return (
    <ValueActions element={element} field={field} queryId={queryId} type={type} value={value}>
      {field} = {_renderTypeSpecific(truncate(value), type)}
    </ValueActions>
  );
};

Value.propTypes = {
  children: PropTypes.node,
  field: PropTypes.string.isRequired,
  queryId: PropTypes.string.isRequired,
  type: CustomPropTypes.FieldType,
  value: PropTypes.node.isRequired,
};

Value.defaultProps = {
  children: null,
  interactive: false,
  type: FieldType.Unknown,
  viewId: null,
};

export default Value;