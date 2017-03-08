import React from 'react';
import { Alert } from 'react-bootstrap';

import { Spinner } from 'components/common';

import loadingIndicatorStyle from './LoadingIndicator.css';

const LoadingIndicator = React.createClass({
  propTypes: {
    text: React.PropTypes.string,
  },

  getDefaultProps() {
    return {
      text: 'Loading...',
    };
  },

  render() {
    return <Alert bsStyle="info" className={loadingIndicatorStyle.loadingIndicator}><Spinner text={this.props.text} /></Alert>;
  },
});

export default LoadingIndicator;
