import React, { PropTypes } from 'react';
import { OverlayTrigger, Popover } from 'react-bootstrap';
import DateTime from 'logic/datetimes/DateTime';
import DayPicker from 'react-day-picker';

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
      try {
        selectedDate = DateTime.parseFromString(this.props.date);
      } catch (e) {
        // don't do anything
      }
    }

    const modifiers = {
      selected: (date) => {
        if (!selectedDate) {
          return false;
        }
        const dateTime = DateTime.ignoreTZ(date);
        return (selectedDate.toString(DateTime.Formats.DATE) === dateTime.toString(DateTime.Formats.DATE));
      },
    };

    const dayPickerFrom = (
      <Popover id={this.props.id} placement="bottom" positionTop={25} title="">
        <DayPicker initialMonth={selectedDate ? selectedDate.toDate() : undefined}
                   onDayClick={this.props.onChange}
                   modifiers={modifiers}
                   enableOutsideDays />
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
