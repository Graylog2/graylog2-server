import React from 'react';
import PropTypes from 'prop-types';

import Select from 'components/common/Select';

const StreamsFilter = ({ disabled, value, streams, onChange }) => {
  const selectedStreams = value.join(',');
  return (
    <div style={{ position: 'relative', zIndex: 10 }} data-testid="streams-filter">
      <Select placeholder="Select streams the search should include. Searches in all streams if empty."
              disabled={disabled}
              displayKey="key"
              inputId="streams-filter"
              onChange={selected => onChange(selected === '' ? [] : selected.split(','))}
              options={streams}
              multi
              style={{ width: '100%' }}
              value={selectedStreams} />
    </div>
  );
};

StreamsFilter.propTypes = {
  disabled: PropTypes.bool,
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
  disabled: false,
  value: [],
};

export default StreamsFilter;
