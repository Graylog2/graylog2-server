import React from 'react';
import PropTypes from 'prop-types';

import styles from './FieldTypeIcon.css';
import FieldType from '../../logic/fieldtypes/FieldType';

const iconClass = (type) => {
  switch (type) {
    case 'string':
      return 'font';
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
const FieldTypeIcon = ({ type }) => {
  return <i className={`fa fa-${iconClass(type.type)} ${styles.fieldTypeIcon}`} />;
};

FieldTypeIcon.propTypes = {
  type: PropTypes.instanceOf(FieldType).isRequired,
};

export default FieldTypeIcon;
