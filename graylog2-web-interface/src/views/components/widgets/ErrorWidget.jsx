import React from 'react';

import { Icon } from 'components/graylog';
import { WidgetErrorsList } from './WidgetPropTypes';

import styles from './MessageWidgets.css';

const ErrorWidget = ({ errors }) => (
  <div className={styles.spinnerContainer}>
    <Icon className={`fa fa-exclamation-triangle fa-3x ${styles.iconMargin}`} />
    <span>
      <strong>While retrieving data for this widget, the following error(s) occurred:</strong>

      <ul>
        {errors.map(e => <li key={e.description}>{e.description}</li>)}
      </ul>
    </span>
  </div>
);

ErrorWidget.propTypes = {
  errors: WidgetErrorsList.isRequired,
};

export default ErrorWidget;
