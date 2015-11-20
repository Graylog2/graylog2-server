import React from 'react';

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
  getValue() {
    return {
      grace: Number(this.refs.grace.value),
      backlog: Number(this.refs.backlog.value),
    };
  },
  render() {
    const parameters = this.props.alertCondition;
    return (
      <span>
         and <br /> then wait at least{' '}
        <input ref="grace" name="grace" type="number" className="form-control pluralsingular validatable"
               data-pluralsingular="grace-descr" data-validate="not_negative_number" defaultValue={parameters.grace}/>{' '}
        <span className="grace-descr" data-plural="minutes" data-singular="minute">minutes</span> until triggering
          a new alert. (grace period)

          <br />

          When sending an alert, include the last{' '}
        <input ref="backlog" name="backlog" type="number" className="form-control pluralsingular validatable"
               data-pluralsingular="backlog-descr" data-validate="not_negative_number" defaultValue={parameters.backlog}/>{' '}
        <span className="backlog-descr" data-plural="messages" data-singular="message">messages</span> of the stream evaluated for this alert condition.
      </span>
    );
  },
});

export default GracePeriodInput;
