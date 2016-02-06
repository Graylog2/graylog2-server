import React from 'react';

import {Pluralize} from 'components/common';

const GracePeriodInput = React.createClass({
  propTypes: {
    parameters: React.PropTypes.object,
    alertCondition: React.PropTypes.object.isRequired,
  },
  getDefaultProps() {
    return {
      parameters: {
        grace: 0,
        backlog: 0,
      },
    };
  },
  getInitialState() {
    return {
      grace: this.props.parameters.grace,
      backlog: this.props.parameters.backlog,
    };
  },
  getValue() {
    return {
      grace: Number(this.refs.grace.value),
      backlog: Number(this.refs.backlog.value),
    };
  },
  _getDefaultValue(field) {
    return this.props.alertCondition[field] || this.props.parameters[field];
  },
  _onGraceChange(event) {
    this.setState({grace: event.target.value});
  },
  _onBacklogChange(event) {
    this.setState({backlog: event.target.value});
  },
  render() {
    return (
      <span>
        and <br /> then wait at least{' '}
        <input ref="grace" name="grace" type="number" min="0" className="form-control"
               defaultValue={this._getDefaultValue('grace')} onChange={this._onGraceChange} required/>
        {' '}
        <Pluralize singular="minute" plural="minutes" value={this.state.grace}/> until triggering a new alert. (grace period)
        <br />

        When sending an alert, include the last{' '}
        <input ref="backlog" name="backlog" type="number" min="0" className="form-control"
               defaultValue={this._getDefaultValue('backlog')} onChange={this._onBacklogChange} required/>
        {' '}
        <Pluralize singular="message" plural="messages" value={this.state.backlog}/> of the stream evaluated for this alert condition.
      </span>
    );
  },
});

export default GracePeriodInput;
