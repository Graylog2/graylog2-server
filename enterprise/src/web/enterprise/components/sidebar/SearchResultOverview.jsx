import React from 'react';
import PropTypes from 'prop-types';
import numeral from 'numeral';

import { Timestamp } from 'components/common';
import DateTime from 'logic/datetimes/DateTime';

const findMessages = (results) => {
  return Object.keys(results.searchTypes)
    .map(id => results.searchTypes[id])
    .find(searchType => searchType.type.toLocaleLowerCase() === 'messages');
};

const SearchResultOverview = ({ results }) => {
  const messages = findMessages(results);
  const duration = numeral(results.duration).format('0,0');
  const timestamp = results.timestamp;
  return (
    <span>
      Found <strong>{numeral(messages.total).format('0,0')} messages</strong> in {numeral(duration).format('0,0')}ms.
      <br />
      Query executed at <Timestamp dateTime={timestamp} format={DateTime.Formats.DATETIME} />.
    </span>
  );
};

SearchResultOverview.propTypes = {
  results: PropTypes.object.isRequired,
};

export default SearchResultOverview;
