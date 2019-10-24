import React from 'react';
import PropTypes from 'prop-types';

import Select from 'components/common/Select';
import { defaultCompare } from 'views/logic/DefaultCompare';

const StreamsFilter = ({ value, streams, onChange }) => {
  const selectedStreams = value.join(',');
  const placeholder = 'Select streams the search should include. Searches in all streams if empty.';
  const options = streams.sort(({ key: key1 }, { key: key2 }) => defaultCompare(key1, key2));
  return (
    <div style={{ position: 'relative', zIndex: 10 }} title={placeholder}>
      <Select placeholder={placeholder}
              displayKey="key"
              onChange={selected => onChange(selected === '' ? [] : selected.split(','))}
              options={options}
              multi
              style={{ width: '100%' }}
              value={selectedStreams} />
    </div>
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
