import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import FieldType from 'views/logic/fieldtypes/FieldType';

const iconClass = (type) => {
  switch (type) {
    case 'string':
      return 'font';
    case 'boolean':
      return 'toggle-on';
    case 'byte':
    case 'double':
    case 'float':
    case 'int':
    case 'long':
    case 'short':
      return 'line-chart';
    case 'date':
      return 'calendar';
    default:
      return 'question-circle';
  }
};

const StyledIcon = styled.i`
  opacity: 0.7;
  margin-right: 9px;
`;

const FieldTypeIcon = ({ type }) => {
  return <StyledIcon className={`fa fa-fw fa-${iconClass(type.type)}`} />;
};

FieldTypeIcon.propTypes = {
  type: PropTypes.instanceOf(FieldType).isRequired,
};

export default FieldTypeIcon;
