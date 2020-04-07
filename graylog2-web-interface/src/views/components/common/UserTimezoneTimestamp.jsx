import React, { useContext } from 'react';
import { ActionContext } from 'views/logic/ActionContext';

import Timestamp from 'components/common/Timestamp';
import DateTime from 'logic/datetimes/DateTime';

const UserTimezoneTimestamp = ({ ...rest }) => {
  const { currentUser: { timezone } = { timezone: 'UTC' } } = useContext(ActionContext);

  return <Timestamp tz={timezone} format={DateTime.Formats.DATETIME_TZ} {...rest} />;
};

export default UserTimezoneTimestamp;
