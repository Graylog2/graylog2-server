import * as React from 'react';
import type { Moment } from 'moment';

import type { DateTimeFormats } from 'util/DateTime';
import useUserDateTime from 'hooks/useUserDateTime';
import { adjustFormat } from 'util/DateTime';

type RenderProps = { value: string, field: string | undefined };

type Props = {
  dateTime?: string | number | Date | Moment | undefined
  field?: string,
  format?: DateTimeFormats,
  render?: React.ComponentType<RenderProps>,
  tz?: string,
  className?: string,
}

/**
 * Component that renders a given date time based on the user time zone in a `time` HTML element.
 * It is capable of render date times in different formats, accepting ISO 8601
 * strings, JS native Date objects, and Moment.js Date objects. On hover the component displays the time in UTC.
 *
 * While the component is using the user time zone by default, it is also possible
 * to change the time zone for the given date, something that helps, for instance, to display a local time
 * from a UTC time that was used in the server.
 *
 */
const Timestamp = ({ dateTime, field, format = 'default', render: Component = ({ value }: RenderProps) => <>{value}</>, tz, className }: Props) => {
  const { formatTime: formatWithUserTz } = useUserDateTime();

  if (!dateTime) {
    // eslint-disable-next-line react/jsx-no-useless-fragment
    return <></>;
  }

  const formattedDateTime = tz ? adjustFormat(dateTime, format, tz) : formatWithUserTz(dateTime, format);
  const dateTimeString = adjustFormat(dateTime, 'internal');

  return (
    <time dateTime={dateTimeString} title={dateTimeString} className={className}>
      <Component value={formattedDateTime} field={field} />
    </time>
  );
};

export default Timestamp;
