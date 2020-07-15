// @flow strict
import * as React from 'react';
import { useState, useEffect } from 'react';
import PropTypes from 'prop-types';

import { Row, Col, HelpBlock } from 'components/graylog';
import { Input } from 'components/bootstrap';
import TimeoutUnitSelect from 'components/users/TimeoutUnitSelect';

type Props = {
  value: number,
  onChange: (value: number) => void;
};

const _estimateUnit = (value) => {
  const MS_DAY = 24 * 60 * 60 * 1000;
  const MS_HOUR = 60 * 60 * 1000;
  const MS_MINUTE = 60 * 1000;
  const MS_SECOND = 1000;

  if (value === 0) {
    return MS_SECOND;
  }

  if (value % MS_DAY === 0) {
    return MS_DAY;
  }

  if (value % MS_HOUR === 0) {
    return MS_HOUR;
  }

  if (value % MS_MINUTE === 0) {
    return MS_MINUTE;
  }

  return MS_SECOND;
};

const TimeoutInput = ({ value: propsValue, onChange }: Props) => {
  const [sessionTimeoutNever, setSessionTimeoutNever] = useState(propsValue === -1);
  const [unit, setUnit] = useState(_estimateUnit(propsValue));
  const [value, setValue] = useState(propsValue ? Math.floor(propsValue / unit) : 0);

  const getValue = () => {
    if (sessionTimeoutNever) {
      return -1;
    }

    return (value * unit);
  };

  useEffect(() => {
    if (typeof onChange === 'function') {
      onChange(getValue());
    }
  }, [value, unit, sessionTimeoutNever]);

  const _onClick = (evt) => {
    setSessionTimeoutNever(evt.target.checked);
  };

  const _onChangeValue = (evt) => {
    setValue(evt.target.value);
  };

  const _onChangeUnit = (evt) => {
    setUnit(evt.target.value);
  };

  return (
    <>
      <Input id="timeout-controls"
             labelClassName="col-sm-3"
             wrapperClassName="col-sm-9"
             label="Sessions Timeout">
        <Row className="no-bm">
          <Col xs={12}>
            <Input type="checkbox"
                   id="session-timeout-never"
                   name="session_timeout_never"
                   label="Sessions do not time out"
                   labelClassName="col-sm-3"
                   wrapperClassName="col-sm-9"
                   help="When checked sessions never time out due to inactivity."
                   onChange={_onClick}
                   checked={sessionTimeoutNever} />
          </Col>
          <Col xs={3}>
            <input type="number"
                   id="timeout"
                   className="session-timeout-fields validatable form-control"
                   name="timeout"
                   min={1}
                   data-validate="positive_number"
                   disabled={sessionTimeoutNever}
                   value={value}
                   onChange={_onChangeValue} />
          </Col>
          <Col xs={4}>
            <TimeoutUnitSelect className="form-control session-timeout-fields"
                               disabled={sessionTimeoutNever}
                               value={unit}
                               onChange={_onChangeUnit} />
          </Col>
          <Col xs={12}>
            <HelpBlock>
              Session automatically end after this amount of time, unless they are actively used.
            </HelpBlock>
          </Col>
        </Row>
      </Input>
    </>
  );
};

TimeoutInput.propTypes = {
  value: PropTypes.number,
  onChange: PropTypes.func,
};

TimeoutInput.defaultProps = {
  value: 60 * 60 * 1000,
  onChange: undefined,
};

export default TimeoutInput;
