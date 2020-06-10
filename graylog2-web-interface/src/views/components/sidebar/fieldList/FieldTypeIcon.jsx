import React from 'react';
import PropTypes from 'prop-types';

import { Icon } from 'components/common';

import FieldType from 'views/logic/fieldtypes/FieldType';
import styles from './FieldTypeIcon.css';

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
const FieldTypeIcon = ({ type }) => {
  return <Icon name={iconClass(type.type)} className={styles.fieldTypeIcon} />;
};

FieldTypeIcon.propTypes = {
  type: PropTypes.instanceOf(FieldType).isRequired,
};

export default FieldTypeIcon;
