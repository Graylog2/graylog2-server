// @flow strict
import React, { useContext } from 'react';
import PropTypes from 'prop-types';
import numeral from 'numeral';

import AppConfig from 'util/AppConfig';
import { Timestamp } from 'components/common';
import CurrentUserContext from 'contexts/CurrentUserContext';
import DateTime from 'logic/datetimes/DateTime';

type Props = {
  results: {
    timestamp?: string,
    duration?: number,
  },
};

const SearchResultOverview = ({ results: { timestamp, duration } }: Props) => {
  const currentUser = useContext(CurrentUserContext);
  const timezone = currentUser?.timezone ?? AppConfig.rootTimeZone();

  if (!timestamp || !duration) {
    return <i>No query executed yet.</i>;
  }

  return (
    <span>
      Query executed in {numeral(duration).format('0,0')}ms at <Timestamp dateTime={timestamp} format={DateTime.Formats.DATETIME} tz={timezone} />.
    </span>
  );
};

SearchResultOverview.propTypes = {
  results: PropTypes.object.isRequired,
};

export default SearchResultOverview;
