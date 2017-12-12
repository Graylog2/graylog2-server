import React from 'react';
import createReactClass from 'create-react-class';
import PropTypes from 'prop-types';

const SearchResultOverview = createReactClass({
  propTypes: {
    job: PropTypes.object.isRequired,
  },

  render() {
    if (!this.props.job) {
      return <span>Not run yet placeholder</span>;
    }
    return (
      <div>
        <h3>Search</h3>
        <span>Executed {Object.keys(this.props.job.results).length} searches</span>
      </div>
    );
  },
});

export default SearchResultOverview;
