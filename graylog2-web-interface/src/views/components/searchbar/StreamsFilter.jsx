import React from 'react';
import PropTypes from 'prop-types';

import Select from 'components/common/Select';
import { defaultCompare } from 'views/logic/DefaultCompare';

const StreamsFilter = ({ disabled, value, streams, onChange }) => {
  const selectedStreams = value.join(',');
  const placeholder = 'Select streams the search should include. Searches in all streams if empty.';
  const options = streams.sort(({ key: key1 }, { key: key2 }) => defaultCompare(key1, key2));
  return (
    <div style={{ position: 'relative', zIndex: 10 }} data-testid="streams-filter" title={placeholder}>
      <Select placeholder={placeholder}
              disabled={disabled}
              displayKey="key"
              inputId="streams-filter"
              onChange={(selected) => onChange(selected === '' ? [] : selected.split(','))}
              options={options}
              multi
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
