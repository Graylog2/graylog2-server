import React from 'react';
import PropTypes from 'prop-types';

class StatusIndicator extends React.Component {
  static propTypes = {
    status: PropTypes.number.isRequired,
  };

  render() {
    let text;
    let icon;
    let className;
    switch (this.props.status) {
      case 0:
        text = 'Running';
        className = 'text-success';
        icon = 'fa-play';
        break;
      case 2:
        text = 'Failing';
        className = 'text-danger';
        icon = 'fa-exclamation-triangle';
        break;
      default:
        text = 'Unknown';
        className = 'text-info';
        icon = 'fa-question-circle';
    }

    return (
      <span className={`${className}`}><i className={`fa ${icon}`} /> {text}</span>
    );

  }
}

export default StatusIndicator;
