import React from 'react';
import PropTypes from 'prop-types';

import connect from 'stores/connect';
import CombinedProvider from 'injection/CombinedProvider';
import Timestamp from 'components/common/Timestamp';
import DateTime from 'logic/datetimes/DateTime';

const { CurrentUserStore } = CombinedProvider.get('CurrentUser');

const UserTimezoneTimestamp = ({ timezone, ...rest }) => <Timestamp tz={timezone} format={DateTime.Formats.DATETIME_TZ} {...rest} />;

UserTimezoneTimestamp.propTypes = {
  timezone: PropTypes.string,
};

UserTimezoneTimestamp.defaultProps = {
  timezone: null,
};

export default connect(
  UserTimezoneTimestamp,
  { currentUser: CurrentUserStore },
  ({ currentUser = { timezone: 'UTC' } }) => ({ timezone: currentUser.timezone }),
);
