import PropTypes from 'prop-types';
import React from 'react';

import { OverlayTrigger, Popover } from 'react-bootstrap';

import Styles from './ErrorPopover.css';

class ErrorPopover extends React.Component {
  static propTypes = {
    errorText: PropTypes.string.isRequired,
    title: PropTypes.string,
    placement: PropTypes.string,
  };

  static defaultProps = {
    title: 'Error',
    placement: 'bottom',
  };

  render() {
    const overlay = (<Popover id="error-popover" title={this.props.title} className={Styles.overlay}>
      {this.props.errorText}
    </Popover>);

    return (
      <OverlayTrigger trigger={['hover', 'focus']} placement={this.props.placement} overlay={overlay}>
        <span className={Styles.trigger}>
          <i className="fa fa-warning text-danger" />
        </span>
      </OverlayTrigger>
    );
  }
}

export default ErrorPopover;
