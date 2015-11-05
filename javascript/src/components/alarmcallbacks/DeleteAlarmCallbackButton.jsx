import React from 'react';
import { Button } from 'react-bootstrap';

const DeleteAlarmCallbackButton = React.createClass({
  propTypes: {
    onClick: React.PropTypes.func.isRequired,
  },
  handleClick(evt) {
    if(window.confirm('Really delete alarm destination?')) {
      this.props.onClick(this.props.alarmCallback);
    }
  },
  render() {
    return (
      <Button bsStyle="danger" onClick={this.handleClick}>
        Delete callback
      </Button>
    );
  },
});

export default DeleteAlarmCallbackButton;
