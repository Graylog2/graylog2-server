import React from 'react';
import PropTypes from 'prop-types';

import styles from './DescriptionBox.css';

const DescriptionBox = ({ children, description }) => (
  <div className={styles.descriptionBox}>
    <div className={styles.description}>{description}</div>
    {children}
  </div>
);

DescriptionBox.propTypes = {
  children: PropTypes.node.isRequired,
  description: PropTypes.string.isRequired,
};

export default DescriptionBox;
