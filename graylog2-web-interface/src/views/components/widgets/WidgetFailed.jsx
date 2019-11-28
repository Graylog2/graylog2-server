// @flow strict
import * as React from 'react';

import { Icon } from 'components/common';
import styles from './MessageWidgets.css';

type Props = {
  error: Error,
};
const WidgetFailed = ({ error }: Props) => (
  <div className={styles.spinnerContainer}>
    <Icon name="exclamation-triangle" size="3x" className={styles.iconMargin} />
    <span>
      <strong>While rendering this widget, the following error occurred:</strong>

      <p>{error.toString()}</p>
    </span>
  </div>
);

WidgetFailed.propTypes = {};

export default WidgetFailed;
