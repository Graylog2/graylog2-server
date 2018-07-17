import React from 'react';
import PropTypes from 'prop-types';

import styles from './LoadingWidget.css';

const LoadingWidget = () => (
  <div className={styles.spinnerContainer}>
    <i className="fa fa-spin fa-3x fa-refresh spinner" />
  </div>
);

LoadingWidget.propTypes = {};

export default LoadingWidget;
