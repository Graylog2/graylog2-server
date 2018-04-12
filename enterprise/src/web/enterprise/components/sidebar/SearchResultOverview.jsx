import React from 'react';
import PropTypes from 'prop-types';
import numeral from 'numeral';

const findMessages = (results) => {
  return Object.keys(results.searchTypes)
    .map(id => results.searchTypes[id])
    .find(searchType => searchType.type.toLocaleLowerCase() === 'messages');
};

const SearchResultOverview = ({ results }) => {
  const messages = findMessages(results);
  const duration = numeral(results.duration).format('0,0');
  return (
    <span>
      Found <strong>{numeral(messages.total).format('0,0')} messages</strong> in {duration}ms.
    </span>
  );
};

SearchResultOverview.propTypes = {
  results: PropTypes.object.isRequired,
};

export default SearchResultOverview;
