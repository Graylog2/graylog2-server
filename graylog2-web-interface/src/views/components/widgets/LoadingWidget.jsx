import React from 'react';

import { Icon } from 'components/common';
import styles from './MessageWidgets.css';

const LoadingWidget = () => (
  <div className={styles.spinnerContainer}>
    <Icon data-testid="loading-widget" name="refresh" size="3x" className="spinner" />
  </div>
);

LoadingWidget.propTypes = {};

export default LoadingWidget;
