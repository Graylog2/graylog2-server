import React from 'react';
import PropTypes from 'prop-types';

import Select from 'components/common/Select';

const StreamsFilter = ({ value, streams, onChange }) => {
  const selectedStreams = value.join(',');
  return (
    <Select placeholder="Select streams the search should include. Searches in all streams if empty."
            displayKey="key"
            onChange={selected => onChange(selected === '' ? [] : selected.split(','))}
            options={streams}
            multi
            style={{ width: '100%' }}
            value={selectedStreams} />
  );
};

StreamsFilter.propTypes = {
  value: PropTypes.arrayOf(PropTypes.string),
  streams: PropTypes.arrayOf(
    PropTypes.shape({
      key: PropTypes.string.isRequired,
      value: PropTypes.string.isRequired,
    }),
  ).isRequired,
  onChange: PropTypes.func.isRequired,
};

StreamsFilter.defaultProps = {
  value: [],
};

export default StreamsFilter;
