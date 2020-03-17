import PropTypes from 'prop-types';
import React from 'react';
import { Alert } from 'components/graylog';

import Spinner from 'components/common/Spinner';

import loadingIndicatorStyle from './LoadingIndicator.css';
import Delayed from './Delayed';

/**
 * Component that displays a loading indicator in the page. It uses a CSS fixed position to always appear
 * on the screen.
 *
 * Use this component when you want to load something in the background, but still provide some feedback that
 * an action is happening.
 */
const LoadingIndicator = ({ text }) => (
  <Delayed delay={500}>
    <Alert bsStyle="info" className={loadingIndicatorStyle.loadingIndicator}>
      <Spinner delay={0} text={text} />
    </Alert>
  </Delayed>
);

LoadingIndicator.propTypes = {
  /** Text to display while the indicator is shown. */
  text: PropTypes.string,
};

LoadingIndicator.defaultProps = {
  text: 'Loading...',
};

export default LoadingIndicator;
