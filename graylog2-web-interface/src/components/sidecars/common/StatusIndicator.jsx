import React from 'react';
import PropTypes from 'prop-types';
import { Tooltip, OverlayTrigger } from 'react-bootstrap';
import lodash from 'lodash';

import SidecarStatusEnum from 'logic/sidecar/SidecarStatusEnum';
import DateTime from 'logic/datetimes/DateTime';

import style from './StatusIndicator.css';

class StatusIndicator extends React.Component {
  static propTypes = {
    id: PropTypes.string,
    lastSeen: PropTypes.string,
    message: PropTypes.string,
    status: PropTypes.number,
  };

  static defaultProps = {
    id: '',
    lastSeen: undefined,
    message: '',
    status: -1,
  };

  render() {
    const text = lodash.upperFirst(SidecarStatusEnum.toString(this.props.status));
    const lastSeenDateTime = new DateTime(this.props.lastSeen);

    let icon;
    let className;
    let { message } = this.props;

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
        message += ` (${lastSeenDateTime.toRelativeString()})`;
    }

    if (this.props.message && this.props.id) {
      const tooltip = <Tooltip id={`${this.props.id}-status-tooltip`}>{message}</Tooltip>;
      return (
        <OverlayTrigger placement="top" overlay={tooltip} rootClose>
          <span className={`${className} ${style.indicator}`}><i className={`fa ${icon} fa-fw`} /> {text}</span>
        </OverlayTrigger>
      );
    }
    return (
      <span className={`${className} ${style.indicator}`}><i className={`fa ${icon} fa-fw`} /> {text}</span>
    );
  }
}

export default StatusIndicator;
