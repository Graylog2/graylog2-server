import React from 'react';
import PropTypes from 'prop-types';

import styles from './FieldTypeIcon.css';

const iconClass = (type) => {
  switch (type) {
    case 'keyword':
      return 'font';
    case 'text':
      return 'paragraph';
    case 'long':
      return 'line-chart';
    case 'date':
      return 'calendar';
    default:
      return 'question-circle';
  }
};
const FieldTypeIcon = ({ type }) => {
  return <i className={`fa fa-${iconClass(type)} ${styles.fieldTypeIcon}`} />;
};

FieldTypeIcon.propTypes = {
  type: PropTypes.string.isRequired,
};

export default FieldTypeIcon;
