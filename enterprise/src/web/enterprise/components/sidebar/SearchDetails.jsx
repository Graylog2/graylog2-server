import React from 'react';
import PropTypes from 'prop-types';
import moment from 'moment-timezone';

import DateTime from 'logic/datetimes/DateTime';
import CombinedProvider from 'injection/CombinedProvider';
import connect from 'stores/connect';
import styles from './SearchDetails.css';

const { CurrentUserStore } = CombinedProvider.get('CurrentUser');

// eslint-disable-next-line react/prop-types
const DateTimeTimestamp = ({ dateTime, timezone }) => {
  const momentTime = moment(dateTime).tz(timezone);
  const displayedTime = momentTime.format(DateTime.Formats.TIMESTAMP);
  return <time title={momentTime.format(DateTime.Formats.TIMESTAMP_TZ)} dateTime={momentTime.toISOString()}>{displayedTime}</time>;
};

const UserTimestamp = connect(DateTimeTimestamp, { currentUser: CurrentUserStore }, ({ currentUser }) => ({ timezone: currentUser.currentUser.timezone }));

const SearchDetails = ({ results }) => {
  const { effectiveTimerange } = results;
  return (
    <span>
      Searched effectively from
      <div className={styles.time}><strong><UserTimestamp dateTime={effectiveTimerange.from} /></strong></div>
      to
      <div className={styles.time}><strong><UserTimestamp dateTime={effectiveTimerange.to} /></strong></div>
    </span>
  );
};

SearchDetails.propTypes = {
  results: PropTypes.shape({
    effectiveTimerange: PropTypes.shape({
      from: PropTypes.string.isRequired,
      to: PropTypes.string.isRequired,
    }).isRequired,
  }).isRequired,
};

export default SearchDetails;
