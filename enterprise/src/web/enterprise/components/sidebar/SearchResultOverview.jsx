import React from 'react';
import PropTypes from 'prop-types';

const SearchResultOverview = ({ job }) => {
  if (!job) {
    return <span>Not run yet placeholder</span>;
  }
  return (
    <div>
      <h3>Search</h3>
      <span>Executed {Object.keys(job.results).length} searches</span>
    </div>
  );
};

SearchResultOverview.propTypes = {
  job: PropTypes.object,
};

export default SearchResultOverview;
