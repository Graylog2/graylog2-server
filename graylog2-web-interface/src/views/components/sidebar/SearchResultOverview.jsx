import React, { useContext } from 'react';
import PropTypes from 'prop-types';
import numeral from 'numeral';

import { Timestamp } from 'components/common';
import CurrentUserContext from 'components/contexts/CurrentUserContext';

import DateTime from 'logic/datetimes/DateTime';

const SearchResultOverview = ({ results }) => {
  const { timestamp } = useContext(CurrentUserContext);
  return (
    <span>
      Query executed in {numeral(results.duration).format('0,0')}ms at <Timestamp dateTime={timestamp} format={DateTime.Formats.DATETIME} />.
    </span>
  );
};

SearchResultOverview.propTypes = {
  results: PropTypes.object.isRequired,
};

export default SearchResultOverview;
