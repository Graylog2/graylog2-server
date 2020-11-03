// @flow strict
import * as React from 'react';
import { useState, useEffect } from 'react';
import PropTypes from 'prop-types';

import { Row, Col, HelpBlock } from 'components/graylog';
import { Input } from 'components/bootstrap';
import TimeoutUnitSelect from 'components/users/TimeoutUnitSelect';

import { MS_DAY, MS_HOUR, MS_MINUTE, MS_SECOND } from './timeoutConstants';

type Props = {
  value: number,
  onChange: (value: number) => void;
};

const _estimateUnit = (value) => {
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
  const [value, setValue] = useState(propsValue ? Math.floor(propsValue / Number(unit)) : 0);

  const getValue = () => {
    if (sessionTimeoutNever) {
      return -1;
    }

    return (value * Number(unit));
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

  const _onChangeUnit = (newUnit: string) => {
    setUnit(newUnit);
  };

  return (
    <>
      <Input id="timeout-controls"
             labelClassName="col-sm-3"
             wrapperClassName="col-sm-9"
             label="Sessions Timeout">
        <>
          <Input type="checkbox"
                 id="session-timeout-never"
                 name="session_timeout_never"
                 label="Sessions do not time out"
                 help="When checked sessions never time out due to inactivity."
                 formGroupClassName="no-bm"
                 onChange={_onClick}
                 checked={sessionTimeoutNever} />

          <div className="clearfix">
            <Col xs={2}>
              <Input type="number"
                     id="timeout"
                     placeholder="Timeout amount"
                     className="validatable"
                     name="timeout"
                     min={1}
                     formGroupClassName="form-group no-bm"
                     data-validate="positive_number"
                     disabled={sessionTimeoutNever}
                     value={value}
                     onChange={_onChangeValue} />
            </Col>
            <Col xs={4}>
              <TimeoutUnitSelect disabled={sessionTimeoutNever}
                                 value={`${unit}`}
                                 onChange={_onChangeUnit} />
            </Col>
            <Row className="no-bm">
              <Col xs={12}>
                <HelpBlock>
                  Session automatically end after this amount of time, unless they are actively used.
                </HelpBlock>
              </Col>
            </Row>
          </div>
        </>
      </Input>
    </>
  );
};

TimeoutInput.propTypes = {
  value: PropTypes.number,
  onChange: PropTypes.func,
};

TimeoutInput.defaultProps = {
  value: MS_HOUR,
  onChange: () => {},
};

export default TimeoutInput;
