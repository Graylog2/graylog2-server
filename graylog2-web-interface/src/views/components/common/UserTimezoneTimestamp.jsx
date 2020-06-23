import React, { useContext } from 'react';

import AppConfig from 'util/AppConfig';
import CurrentUserContext from 'contexts/CurrentUserContext';
import Timestamp from 'components/common/Timestamp';
import DateTime from 'logic/datetimes/DateTime';

const UserTimezoneTimestamp = ({ ...rest }) => {
  const currentUser = useContext(CurrentUserContext);
  const timezone = currentUser?.timezone ?? AppConfig.rootTimeZone();
  return <Timestamp tz={timezone} format={DateTime.Formats.DATETIME_TZ} {...rest} />;
};

export default UserTimezoneTimestamp;
