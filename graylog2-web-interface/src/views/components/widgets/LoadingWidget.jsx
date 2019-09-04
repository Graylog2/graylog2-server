import React from 'react';

import { Icon } from 'components/graylog';
import styles from './MessageWidgets.css';

const LoadingWidget = () => (
  <div className={styles.spinnerContainer}>
    <Icon className="fa fa-spin fa-3x fa-refresh spinner" />
  </div>
);

LoadingWidget.propTypes = {};

export default LoadingWidget;
