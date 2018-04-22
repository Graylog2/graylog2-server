import React from 'react';
import PropTypes from 'prop-types';
import { Button, Col, Row } from 'react-bootstrap';

import Input from 'components/bootstrap/Input';
import { DatePicker } from 'components/common';
import DateTime from 'logic/datetimes/DateTime';

const _formattedDateStringInUserTZ = (field, rangeParams) => {
  const dateString = rangeParams.get(field);

  if (dateString === null || dateString === undefined || dateString === '') {
    return dateString;
  }

  // We only format the original dateTime, as datepicker will format the date in another way, and we
  // don't want to annoy users trying to guess what they are typing.
  if (rangeParams.get(field) === dateString) {
    return DateTime.parseFromString(dateString).toString();
  }

  return dateString;
};

const _setDateTimeToNow = (field, onChange) => {
  const value = new DateTime().toString(DateTime.Formats.DATETIME);
  onChange(field, value);
};

const _isValidDateString = (dateString) => {
  try {
    if (dateString !== undefined) {
      DateTime.parseFromString(dateString);
    }
    return true;
  } catch (e) {
    return false;
  }
};

// TODO: Transfer this to AbsoluteTimeRangeSelector
const _rangeParamsChanged = (key) => {
  return () => {
    let refInput;

    switch (key) {
      case 'from':
      case 'to':
        const ref = `${key}Formatted`;
        refInput = this.refs[ref];
        if (!this._isValidDateString(refInput.getValue())) {
          refInput.getInputDOMNode().setCustomValidity('Invalid date time provided');
        } else {
          refInput.getInputDOMNode().setCustomValidity('');
        }
        break;
      default:
        refInput = this.refs[key];
    }
    SearchActions.rangeParams(key, refInput.getValue);
  };
};

const _isValidDateField = (field, rangeParams) => {
  return _isValidDateString(_formattedDateStringInUserTZ(field, rangeParams));
};

const _onDateSelected = (date, key, onChange) => {
  const midnightDate = date.setHours(0);
  const newValue = DateTime.ignoreTZ(midnightDate).toString(DateTime.Formats.DATETIME);
  onChange(key, newValue);
};

export default function AbsoluteTimeRangeSelector({ value, onChange }) {
  return (
    <div className="timerange-selector absolute">
      <Row className="no-bm">
        <Col md={5} style={{ padding: 0 }}>
          <input type="hidden" name="from" />
          <DatePicker id="searchFromDatePicker"
                      title="Search start date"
                      date={value.get('from')}
                      onChange={date => _onDateSelected(date, 'from', onChange)}>
            <Input type="text"
                   className={"absolute"}
                   value={_formattedDateStringInUserTZ('from', value)}
                   onChange={newValue => onChange('from', newValue)}
                   placeholder={DateTime.Formats.DATETIME}
                   buttonAfter={<Button bsSize="small" onClick={() => _setDateTimeToNow('from', onChange)}><i className="fa fa-magic" /></Button>}
                   bsStyle={_isValidDateField('from', value) ? null : 'error'}
                   required />
          </DatePicker>

        </Col>

        <Col md={1}>
          <p className="text-center" style={{ margin: 0, lineHeight: '34px', fontSize: '18px' }}>
            <i className="fa fa-long-arrow-right" />
          </p>
        </Col>

        <Col md={5} style={{ padding: 0 }}>
          <input type="hidden" name="to" />
          <DatePicker id="searchToDatePicker"
                      title="Search end date"
                      date={value.get('to')}
                      onChange={date => _onDateSelected(date, 'to', onChange)}>
            <Input type="text"
                   className={"absolute"}
                   value={_formattedDateStringInUserTZ('to', value)}
                   onChange={newValue => onChange('to', newValue)}
                   placeholder={DateTime.Formats.DATETIME}
                   buttonAfter={<Button bsSize="small" onClick={() => _setDateTimeToNow('to', onChange)}><i className="fa fa-magic" /></Button>}
                   bsStyle={_isValidDateField('to', value) ? null : 'error'}
                   required />
          </DatePicker>
        </Col>
      </Row>
    </div>
  );
}

AbsoluteTimeRangeSelector.propTypes = {
  value: PropTypes.object.isRequired,
  onChange: PropTypes.func.isRequired,
};
