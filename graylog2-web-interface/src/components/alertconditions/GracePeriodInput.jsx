import React from 'react';

import { Pluralize } from 'components/common';

const GracePeriodInput = React.createClass({
  propTypes: {
    parameters: React.PropTypes.object.isRequired,
  },
  getInitialState() {
    return {
      grace: this.props.parameters.grace,
      backlog: this.props.parameters.backlog,
    };
  },
  getValue() {
    return this.state;
  },
  _onChange(event) {
    const state = {};
    state[event.target.name] = event.target.value;
    this.setState(state);
  },
  render() {
    return (
      <span>
        and <br /> then wait at least{' '}
        <input ref="grace" name="grace" type="number" min="0" className="form-control"
               value={this.state.grace} onChange={this._onChange} required />
        {' '}
        <Pluralize singular="minute" plural="minutes" value={this.state.grace} /> until triggering a new alert. (grace period)
        <br />

        When sending an alert, include the last{' '}
        <input ref="backlog" name="backlog" type="number" min="0" className="form-control"
               value={this.state.backlog} onChange={this._onChange} required />
        {' '}
        <Pluralize singular="message" plural="messages" value={this.state.backlog} /> of the stream evaluated for this alert condition.
      </span>
    );
  },
});

export default GracePeriodInput;
