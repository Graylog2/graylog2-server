// @flow strict
import React from 'react';
import PropTypes from 'prop-types';

import { Icon } from 'components/common';
import { WidgetErrorsList } from './WidgetPropTypes';

import styles from './MessageWidgets.css';

type WidgetError = {
  description: string,
};

type Props = {
  errors: Array<WidgetError>,
  title?: string,
};

const ErrorWidget = ({ errors, title }: Props) => (
  <div className={styles.spinnerContainer}>
    <Icon name="exclamation-triangle" size="3x" className={styles.iconMargin} />
    <span>
      <strong>{title}</strong>

      <ul>
        {errors.map(e => <li key={e.description}>{e.description}</li>)}
      </ul>
    </span>
  </div>
);

ErrorWidget.propTypes = {
  errors: WidgetErrorsList.isRequired,
  title: PropTypes.string,
};

ErrorWidget.defaultProps = {
  title: 'While retrieving data for this widget, the following error(s) occurred:',
};

export default ErrorWidget;
