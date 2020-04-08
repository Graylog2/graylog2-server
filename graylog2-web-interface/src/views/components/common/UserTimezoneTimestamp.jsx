import React, { useContext } from 'react';
import CurrentUserContext from 'components/contexts/CurrentUserContext';

import Timestamp from 'components/common/Timestamp';
import DateTime from 'logic/datetimes/DateTime';

const UserTimezoneTimestamp = ({ ...rest }) => {
  const currentUser = useContext(CurrentUserContext);
  const timezone = currentUser ? currentUser.timezone : 'UTC';
  return <Timestamp tz={timezone} format={DateTime.Formats.DATETIME_TZ} {...rest} />;
};

export default UserTimezoneTimestamp;
