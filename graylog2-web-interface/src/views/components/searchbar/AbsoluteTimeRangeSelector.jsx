import React from 'react';
import PropTypes from 'prop-types';
import Immutable from 'immutable';

import { Button } from 'components/graylog';
import Input from 'components/bootstrap/Input';
import { DatePicker } from 'components/common';
import DateTime from 'logic/datetimes/DateTime';

import styles from './AbsoluteTimeRangeSelector.css';

const _formattedDateStringInUserTZ = (dateString) => {
  return DateTime.parseFromString(dateString);
};

const _setDateTimeToNow = (field, onChange) => {
  const value = new DateTime().toISOString();
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

const _onDateSelected = (date, key, onChange) => {
  const midnightDate = date.setHours(0);
  const newValue = DateTime.ignoreTZ(midnightDate).toISOString();
  onChange(key, newValue);
};

const _extractStateFromProps = (props) => {
  const { from, to } = props.value.map(value => _formattedDateStringInUserTZ(value).toString()).toObject();
  return {
    from,
    to,
  };
};

export default class AbsoluteTimeRangeSelector extends React.Component {
  constructor(props, context) {
    super(props, context);

    this.state = _extractStateFromProps(props);
  }

  componentWillReceiveProps(nextProps) {
    if (!Immutable.is(nextProps.value, this.props.value)) {
      this.setState(_extractStateFromProps(nextProps));
    }
  }

  onBlur = (key, onChange) => {
    onChange(key, _formattedDateStringInUserTZ(this.state[key]).toISOString());
  };

  onChange = (key, value) => {
    this.setState({ [key]: value });
  };

  render() {
    const { from, to } = this.state;
    return (
      <div className={`timerange-selector absolute ${styles.selectorContainer}`}>
        <input type="hidden" name="from" />
        <div className={styles.inputWidth}>
          <DatePicker id="searchFromDatePicker"
                      title="Search start date"
                      date={from}
                      onChange={date => _onDateSelected(date, 'from', this.props.onChange)}>
            <Input id="fromDateInput"
                   type="text"
                   className="absolute"
                   value={from}
                   onBlur={() => this.onBlur('from', this.props.onChange)}
                   onChange={event => this.onChange('from', event.target.value)}
                   placeholder={DateTime.Formats.DATETIME}
                   buttonAfter={(
                     <Button onClick={() => _setDateTimeToNow('from', this.props.onChange)}><i className="fa fa-magic" />
                     </Button>
)}
                   bsStyle={_isValidDateString(from) ? null : 'error'}
                   required />
          </DatePicker>
        </div>

        <p className={`text-center ${styles.separator}`}>
          <i className="fa fa-long-arrow-right" />
        </p>
        <input type="hidden" name="to" />
        <div className={styles.inputWidth}>
          <DatePicker id="searchToDatePicker"
                      title="Search end date"
                      date={to}
                      onChange={date => _onDateSelected(date, 'to', this.props.onChange)}>
            <Input id="toDateInput"
                   type="text"
                   className="absolute"
                   value={to}
                   onBlur={() => this.onBlur('to', this.props.onChange)}
                   onChange={event => this.onChange('to', event.target.value)}
                   placeholder={DateTime.Formats.DATETIME}
                   buttonAfter={(
                     <Button onClick={() => _setDateTimeToNow('to', this.props.onChange)}><i className="fa fa-magic" />
                     </Button>
)}
                   bsStyle={_isValidDateString(to) ? null : 'error'}
                   required />
          </DatePicker>
        </div>
      </div>
    );
  }
}

AbsoluteTimeRangeSelector.propTypes = {
  value: PropTypes.object.isRequired,
  onChange: PropTypes.func.isRequired,
};
