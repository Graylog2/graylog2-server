import PropTypes from 'prop-types';
import React from 'react';
import { Alert } from 'react-bootstrap';

import { Spinner } from 'components/common';

import loadingIndicatorStyle from './LoadingIndicator.css';

/**
 * Component that displays a loading indicator in the page. It uses a CSS fixed position to always appear
 * on the screen.
 *
 * Use this component when you want to load something in the background, but still provide some feedback that
 * an action is happening.
 */
class LoadingIndicator extends React.Component {
  static propTypes = {
    /** Text to display while the indicator is shown. */
    text: PropTypes.string,
  };

  static defaultProps = {
    text: 'Loading...',
  };

  render() {
    return <Alert bsStyle="info" className={loadingIndicatorStyle.loadingIndicator}><Spinner text={this.props.text} /></Alert>;
  }
}

export default LoadingIndicator;
