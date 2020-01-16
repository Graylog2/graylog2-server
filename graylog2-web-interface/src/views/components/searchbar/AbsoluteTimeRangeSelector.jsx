import React from 'react';
import PropTypes from 'prop-types';
import Immutable from 'immutable';

import { Button } from 'components/graylog';
import Input from 'components/bootstrap/Input';
import { DatePicker, Icon } from 'components/common';
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
    const { value } = this.props;

    if (!Immutable.is(nextProps.value, value)) {
      this.setState(_extractStateFromProps(nextProps));
    }
  }

  onBlur = (key, onChange) => {
    // eslint-disable-next-line react/destructuring-assignment
    const value = this.state[key];

    if (_isValidDateString(value)) {
      onChange(key, _formattedDateStringInUserTZ(value).toISOString());
    }
  };

  onChange = (key, value) => {
    this.setState({ [key]: value });
  };

  render() {
    const { from, to } = this.state;
    const { disabled, onChange } = this.props;

    return (
      <div className={`timerange-selector absolute ${styles.selectorContainer}`}>
        <input type="hidden" name="from" />
        <div className={styles.inputWidth}>
          <DatePicker id="searchFromDatePicker"
                      disabled={disabled}
                      title="Search start date"
                      date={from}
                      onChange={date => _onDateSelected(date, 'from', onChange)}>
            <Input id="fromDateInput"
                   type="text"
                   disabled={disabled}
                   className="absolute"
                   value={from}
                   onBlur={() => this.onBlur('from', onChange)}
                   onChange={event => this.onChange('from', event.target.value)}
                   placeholder={DateTime.Formats.DATETIME}
                   buttonAfter={(
                     <Button disabled={disabled} onClick={() => _setDateTimeToNow('from', onChange)}>
                       <Icon name="magic" />
                     </Button>
                   )}
                   bsStyle={_isValidDateString(from) ? null : 'error'}
                   required />
          </DatePicker>
        </div>

        <p className={`text-center ${styles.separator}`}>
          <Icon name="long-arrow-right" />
        </p>
        <input type="hidden" name="to" />
        <div className={styles.inputWidth}>
          <DatePicker id="searchToDatePicker"
                      disabled={disabled}
                      title="Search end date"
                      date={to}
                      onChange={date => _onDateSelected(date, 'to', onChange)}>
            <Input id="toDateInput"
                   type="text"
                   disabled={disabled}
                   className="absolute"
                   value={to}
                   onBlur={() => this.onBlur('to', onChange)}
                   onChange={event => this.onChange('to', event.target.value)}
                   placeholder={DateTime.Formats.DATETIME}
                   buttonAfter={(
                     <Button disabled={disabled} onClick={() => _setDateTimeToNow('to', onChange)}>
                       <Icon name="magic" />
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
  disabled: PropTypes.bool,
  value: PropTypes.object.isRequired,
  onChange: PropTypes.func.isRequired,
};

AbsoluteTimeRangeSelector.defaultProps = {
  disabled: false,
};
