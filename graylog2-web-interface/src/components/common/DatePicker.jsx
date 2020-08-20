import PropTypes from 'prop-types';
import React from 'react';
import DayPicker from 'react-day-picker';
import styled from 'styled-components';

import { OverlayTrigger, Popover } from 'components/graylog';
import DateTime from 'logic/datetimes/DateTime';

import 'react-day-picker/lib/style.css';

const StyledPopover = styled(Popover)`
  .popover-content {
    padding: 0;
  }
`;

const StyledDayPicker = styled(DayPicker)(({ theme }) => `
  .DayPicker-Day {
    min-width: 34px;
    max-width: 34px;
    min-height: 34px;
    max-height: 34px;
    
    &:not(.DayPicker-Day--disabled):not(.DayPicker-Day--selected):not(.DayPicker-Day--outside):hover {
      color: ${theme.colors.variant.default};
    }
  }
`);

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

  static defaultProps = {
    date: undefined,
  }

  render() {
    const { children, date, id, onChange, title } = this.props;
    let selectedDate;

    if (date) {
      try {
        selectedDate = DateTime.parseFromString(date);
      } catch (e) {
        // don't do anything
      }
    }

    const modifiers = {
      selected: (moddedDate) => {
        if (!selectedDate) {
          return false;
        }

        const dateTime = DateTime.ignoreTZ(moddedDate);

        return (selectedDate.toString(DateTime.Formats.DATE) === dateTime.toString(DateTime.Formats.DATE));
      },
    };

    const dayPickerFrom = (
      <StyledPopover id={id}
                     placement="bottom"
                     positionTop={25}
                     title={title}>
        <StyledDayPicker initialMonth={selectedDate ? selectedDate.toDate() : undefined}
                         onDayClick={onChange}
                         modifiers={modifiers}
                         enableOutsideDays />
      </StyledPopover>
    );

    return (
      <OverlayTrigger trigger="click"
                      rootClose
                      placement="bottom"
                      overlay={dayPickerFrom}>
        {children}
      </OverlayTrigger>
    );
  }
}

export default DatePicker;
