import React from 'react';
import PropTypes from 'prop-types';
import { Col, Row } from 'react-bootstrap';

import Select from 'components/common/Select';

const StreamsFilter = ({ value, streams, onChange }) => {
  const selectedStreams = value.join(',');
  return (
      <Select placeholder="Select streams to be searched in"
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
