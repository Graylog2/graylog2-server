import React, { useContext } from 'react';
import PropTypes from 'prop-types';
import numeral from 'numeral';

import { Timestamp } from 'components/common';
import CurrentUserContext from 'components/contexts/CurrentUserContext';

import DateTime from 'logic/datetimes/DateTime';

const SearchResultOverview = ({ results }) => {
  const { timezone } = useContext(CurrentUserContext) || { timezone: 'UTC' };
  const { timestamp } = results;
  return (
    <span>
      Query executed in {numeral(results.duration).format('0,0')}ms at <Timestamp dateTime={timestamp} format={DateTime.Formats.DATETIME} tz={timezone} />.
    </span>
  );
};

SearchResultOverview.propTypes = {
  results: PropTypes.object.isRequired,
};

export default SearchResultOverview;
