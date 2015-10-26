import React from 'react';
import { Input, Row, Col } from 'react-bootstrap';

import TimeoutUnitSelect from 'components/users/TimeoutUnitSelect';

const TimeoutInput = React.createClass({
  getInitialState() {
    return {
      sessionTimeoutNever: false,
    };
  },
  getValue() {
    if (this.state.sessionTimeoutNever) {
      return -1;
    }
    return (this.refs.timeout.value * this.refs.session_timeout_unit.getValue());
  },
  _onClick() {
    this.setState({sessionTimeoutNever: !this.state.sessionTimeoutNever});
  },
  render() {
    return (
      <span>
        <Input ref="session_timeout_never" type="checkbox" id="session-timeout-never" name="session_timeout_never"
               labelClassName="col-sm-10" wrapperClassName="col-sm-offset-2 col-sm-10"
               label="Sessions do not time out" help="When checked sessions never time out due to inactivity."
               onClick={this._onClick} value={this.state.sessionTimeoutNever}/>

        <Input label="Timeout" help="Session automatically end after this amount of time, unless they are actively used."
               labelClassName="col-sm-2" wrapperClassName="col-sm-10">

          <Row>
            <Col sm={2}>
              <input ref="timeout" type="number" id="timeout" className="session-timeout-fields validatable form-control"
                     name="timeout" min={1} data-validate="positive_number" disabled={this.state.sessionTimeoutNever} />
            </Col>
            <Col sm={3}>
              <TimeoutUnitSelect ref="session_timeout_unit" className="form-control session-timeout-fields"
                                 disabled={this.state.sessionTimeoutNever}/>
            </Col>
          </Row>
        </Input>
      </span>
    );
  },
});

export default TimeoutInput;
