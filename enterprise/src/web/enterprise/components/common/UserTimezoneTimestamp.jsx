import React from 'react';
import PropTypes from 'prop-types';

import connect from 'stores/connect';
import CombinedProvider from 'injection/CombinedProvider';
import Timestamp from 'components/common/Timestamp';

const { CurrentUserStore } = CombinedProvider.get('CurrentUser');

const UserTimezoneTimestamp = ({ timezone, ...rest }) => <Timestamp tz={timezone} {...rest} />;

UserTimezoneTimestamp.propTypes = {
  timezone: PropTypes.string.isRequired,
};

export default connect(UserTimezoneTimestamp, { currentUser: CurrentUserStore }, ({ currentUser }) => ({ timezone: currentUser.timezone }));
