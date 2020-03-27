import React from 'react';
import PropTypes from 'prop-types';
import lodash from 'lodash';

import { Tooltip, OverlayTrigger } from 'components/graylog';
import { Icon } from 'components/common';
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
        icon = 'play';
        break;
      case SidecarStatusEnum.FAILING:
        className = 'text-danger';
        icon = 'exclamation-triangle';
        break;
      case SidecarStatusEnum.STOPPED:
        className = 'text-danger';
        icon = 'stop';
        break;
      default:
        className = 'text-info';
        icon = 'question-circle';
        message += ` (${lastSeenDateTime.toRelativeString()})`;
    }

    if (this.props.message && this.props.id) {
      const tooltip = <Tooltip id={`${this.props.id}-status-tooltip`}>{message}</Tooltip>;
      return (
        <OverlayTrigger placement="top" overlay={tooltip} rootClose>
          <span className={`${className} ${style.indicator}`}>
            <Icon name={icon} fixedWidth /> {text}
          </span>
        </OverlayTrigger>
      );
    }
    return (
      <span className={`${className} ${style.indicator}`}>
        <Icon name={icon} fixedWidth /> {text}
      </span>
    );
  }
}

export default StatusIndicator;
