import React from 'react';
import PropTypes from 'prop-types';
import { Tooltip, OverlayTrigger } from 'react-bootstrap';
import lodash from 'lodash';

import SidecarStatusEnum from 'logic/sidecar/SidecarStatusEnum';

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
    const text = lodash.upperFirst(SidecarStatusEnum.toString(this.props.status));
    let icon;
    let className;

    switch (this.props.status) {
      case SidecarStatusEnum.RUNNING:
        className = 'text-success';
        icon = 'fa-play';
        break;
      case SidecarStatusEnum.FAILING:
        className = 'text-danger';
        icon = 'fa-exclamation-triangle';
        break;
      case SidecarStatusEnum.STOPPED:
        className = 'text-danger';
        icon = 'fa-stop';
        break;
      default:
        className = 'text-info';
        icon = 'fa-question-circle';
    }

    if (this.props.message && this.props.id) {
      const tooltip = <Tooltip id={`${this.props.id}-status-tooltip`}>{this.props.message}</Tooltip>;
      return (
        <OverlayTrigger placement="top" overlay={tooltip} rootClose>
          <span className={`${className}`}><i className={`fa ${icon} fa-fw`} /> {text}</span>
        </OverlayTrigger>
      );
    } else {
      return (
        <span className={`${className}`}><i className={`fa ${icon} fa-fw`} /> {text}</span>
      );
    }
  }
}

export default StatusIndicator;
