import React from 'react';
import PropTypes from 'prop-types';
import { Tooltip, OverlayTrigger } from 'react-bootstrap';

class StatusIndicator extends React.Component {
  static propTypes = {
    status: PropTypes.number,
    message: PropTypes.string,
    id: PropTypes.string,
  };

  static defaultProps = {
    status: -1,
    message: '',
    id: '',
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
      case 3:
        text = 'Stopped';
        className = 'text-danger';
        icon = 'fa-stop';
        break;

      default:
        text = 'Unknown';
        className = 'text-info';
        icon = 'fa-question-circle';
    }

    if (this.props.message && this.props.id) {
      const tooltip = <Tooltip id={`${this.props.id}-status-tooltip`}>{this.props.message}</Tooltip>;
      return (
        <OverlayTrigger placement="top" overlay={tooltip} rootClose>
          <span className={`${className}`}><i className={`fa ${icon}`} /> {text}</span>
        </OverlayTrigger>
      );
    } else {
      return (
        <span className={`${className}`}><i className={`fa ${icon}`} /> {text}</span>
      );
    }
  }
}

export default StatusIndicator;
