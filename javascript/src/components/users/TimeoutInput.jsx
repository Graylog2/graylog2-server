import React from 'react';
import { Row, Col } from 'react-bootstrap';

import TimeoutUnitSelect from 'components/users/TimeoutUnitSelect';

const TimeoutInput = React.createClass({
  getValue() {
    return (this.refs.timeout.value * this.refs.session_timeout_unit.getValue());
  },
  render() {
    return (
      <Row className="row">
        <Col sm={2}>
          <input ref="timeout" type="number" id="timeout" className="session-timeout-fields validatable form-control" name="timeout" min={1} data-validate="positive_number" />
        </Col>
        <Col sm={3}>
          <TimeoutUnitSelect ref="session_timeout_unit" className="form-control session-timeout-fields" />
        </Col>
      </Row>
    );
  },
});

export default TimeoutInput;
