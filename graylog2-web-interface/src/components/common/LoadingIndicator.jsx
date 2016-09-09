import React from 'react';

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
    return <div className={loadingIndicatorStyle.loadingIndicator}><Spinner text={this.props.text}/></div>;
  },
});

export default LoadingIndicator;
