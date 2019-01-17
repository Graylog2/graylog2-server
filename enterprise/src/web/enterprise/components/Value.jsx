import React from 'react';
import PropTypes from 'prop-types';

import FieldType from 'enterprise/logic/fieldtypes/FieldType';

import CustomPropTypes from './CustomPropTypes';
import ValueActions from './ValueActions';
import TypeSpecificValue from './TypeSpecificValue';

const Value = ({ children, field, value, queryId, type }) => {
  const caption = <TypeSpecificValue value={value} type={type} />;
  const element = children || caption;

  return (
    <ValueActions element={element} field={field} queryId={queryId} type={type} value={value}>
      {field} = <TypeSpecificValue value={value} type={type} truncate />
    </ValueActions>
  );
};

Value.propTypes = {
  children: PropTypes.node,
  field: PropTypes.string.isRequired,
  queryId: PropTypes.string.isRequired,
  type: CustomPropTypes.FieldType,
  value: PropTypes.any.isRequired,
};

Value.defaultProps = {
  children: null,
  interactive: false,
  type: FieldType.Unknown,
  viewId: null,
};

export default Value;
