import PropTypes from 'prop-types';
import React from 'react';
import { OverlayTrigger, Popover } from 'react-bootstrap';
import DateTime from 'logic/datetimes/DateTime';
import DayPicker from 'react-day-picker';

import 'react-day-picker/lib/style.css';

/**
 * Component that renders a given children and wraps a date picker around it. The date picker will show when
 * the children is clicked, and hidden when clicking somewhere else.
 */
class DatePicker extends React.Component {
  static propTypes = {
    /** Element id to use in the date picker Popover. */
    id: PropTypes.string.isRequired,
    /** Title to use in the date picker Popover.  */
    title: PropTypes.string.isRequired,
    /** Initial date to select in the date picker. */
    date: PropTypes.string,
    /**
     * Callback that will be called when user picks a date. It will receive the new selected day,
     * `react-day-picker`'s modifiers, and the original event as arguments.
     */
    onChange: PropTypes.func.isRequired,
    /** Element that will trigger the date picker Popover. */
    children: PropTypes.node.isRequired,
  };

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
      <Popover id={this.props.id} placement="bottom" positionTop={25} title={this.props.title}>
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
  }
}

export default DatePicker;
