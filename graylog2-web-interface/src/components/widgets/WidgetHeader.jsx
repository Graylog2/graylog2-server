import React, { PropTypes } from 'react';
import { Timestamp } from 'components/common';

const WidgetHeader = React.createClass({
  propTypes: {
    title: PropTypes.string.isRequired,
    error: PropTypes.any,
    errorMessage: PropTypes.string,
    calculatedAt: PropTypes.string,
  },
  render() {
    let loadErrorElement;

    if (this.props.error) {
      loadErrorElement = (
        <span className="load-error" title={this.props.errorMessage}>
          <i className="fa fa-exclamation-triangle" />
        </span>
      );
    }

    let calculatedAtTime;

    if (this.props.calculatedAt) {
      calculatedAtTime = <span title={this.props.calculatedAt}><Timestamp dateTime={this.props.calculatedAt} relative /></span>;
    } else {
      calculatedAtTime = 'Loading...';
    }

    return (
      <div>
        <div className="widget-update-info">
          {loadErrorElement}
          {calculatedAtTime}
        </div>
        <div className="widget-title">
          {this.props.title}
        </div>
        <div className="clearfix" />
      </div>
    );
  },
});

export default WidgetHeader;
