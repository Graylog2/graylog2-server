import React from 'react';
import jQuery from 'jquery';

import GracePeriodInput from 'components/alertconditions/GracePeriodInput';

const MessageCountConditionForm = React.createClass({
  propTypes: {
    alertCondition: React.PropTypes.object,
  },
  getDefaultProps() {
    return {
      alertCondition: {
        threshold_type: 'more',
        threshold: 0,
        time: 0,
      },
    };
  },
  getInitialState() {
    return {
      threshold_type: this.props.alertCondition.threshold_type,
    };
  },
  getValue() {
    return jQuery.extend({
      threshold_type: this.refs.threshold_type.value,
      threshold: Number(this.refs.threshold.value),
      time: Number(this.refs.time.value),
    }, this.refs.gracePeriod.getValue());
  },
  _onTypeChanged(evt) {
    this.setState({threshold_type: evt.target.value});
  },
  render() {
    const alertCondition = this.props.alertCondition;
    return (
        <span>
          Trigger alert when there are
          <span className="threshold-type">
            <label className="radio-inline">
              <input ref="threshold_type" type="radio" name="threshold_type" onChange={this._onTypeChanged} value="more"
                     checked={this.state.threshold_type === 'more'}/>
              more
            </label>

            <label className="radio-inline">
              <input ref="threshold_type" type="radio" name="threshold_type" onChange={this._onTypeChanged} value="less"
                     checked={this.state.threshold_type === 'less'}/>
              less
            </label>
          </span>
          <br />
          than {' '}
          <input ref="threshold" name="threshold" type="number" className="form-control pluralsingular validatable"
                 data-validate="number" data-pluralsingular="threshold-descr" defaultValue={alertCondition.threshold} />
          {' '}<span className="threshold-descr" data-plural="messages" data-singular="message">messages</span>{' '}
          in the last{' '}
          <input ref="time" name="time" type="number" className="form-control pluralsingular validatable"
                 data-validate="positive_number" data-pluralsingular="time-descr" defaultValue={alertCondition.time} />{' '}
          <span className="time-descr" data-plural="minutes" data-singular="minute">minutes</span>
          {' '}
          <GracePeriodInput ref="gracePeriod" {...this.props}/>
        </span>
    );
  },
});

export default MessageCountConditionForm;
