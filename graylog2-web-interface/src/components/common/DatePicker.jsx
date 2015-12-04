import React, {PropTypes} from 'react';
import {OverlayTrigger, Popover} from 'react-bootstrap';
import moment from 'moment';
import DayPicker, { DateUtils } from 'react-day-picker';

import 'react-day-picker/lib/style.css';

const DatePicker = React.createClass({
  propTypes: {
    id: PropTypes.string.isRequired,
    title: PropTypes.string.isRequired,
    date: PropTypes.string,
    dateFormatString: PropTypes.string,
    onChange: PropTypes.func.isRequired,
    children: PropTypes.node.isRequired,
  },
  render() {
    let selectedDate;
    if (this.props.date) {
      selectedDate = moment(this.props.date, this.props.dateFormatString).toDate();
    }

    const dayPickerFrom = (
      <Popover id={this.props.id} placement="bottom" positionTop={25} title="">
        <DayPicker initialMonth={selectedDate}
                   onDayClick={this.props.onChange}
                   modifiers={{
                     selected: date => selectedDate && DateUtils.isSameDay(selectedDate, date),
                   }}
                   enableOutsideDays/>
      </Popover>
    );

    return (
      <OverlayTrigger trigger="click" rootClose placement="bottom" overlay={dayPickerFrom}>
        {this.props.children}
      </OverlayTrigger>
    );
  },
});

export default DatePicker;
