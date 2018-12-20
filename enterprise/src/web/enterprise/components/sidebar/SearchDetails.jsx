import React from 'react';
import PropTypes from 'prop-types';

import Timestamp from 'components/common/Timestamp';
import DateTime from 'logic/datetimes/DateTime';
import styles from './SearchDetails.css';

// eslint-disable-next-line react/prop-types
const DateTimeTimestamp = ({ dateTime }) => {
  const tz = DateTime.getUserTimezone();
  const format = DateTime.Formats.TIMESTAMP;
  return <Timestamp dateTime={dateTime} tz={tz} format={format} />;
};

const SearchDetails = ({ results }) => {
  const { effectiveTimerange } = results;
  return (
    <span>
      Searched effectively from
      <div className={styles.time}><strong><DateTimeTimestamp dateTime={effectiveTimerange.from} /></strong></div>
      to
      <div className={styles.time}><strong><DateTimeTimestamp dateTime={effectiveTimerange.to} /></strong></div>
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
