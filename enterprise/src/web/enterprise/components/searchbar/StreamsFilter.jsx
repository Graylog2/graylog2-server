import React from 'react';
import PropTypes from 'prop-types';
import { Col, Row } from 'react-bootstrap';

import Select from 'components/common/Select';

const StreamsFilter = ({ value, streams, onChange }) => {
  const selectedStreams = value.join(',');
  return (
    <Row className="no-bm">
      <Col md={11} mdOffset={1}>
        <Select placeholder="Select streams to be searched in"
                displayKey="key"
                onChange={selected => onChange(selected === '' ? [] : selected.split(','))}
                options={streams}
                multi
                style={{ width: '90%' }}
                value={selectedStreams} />
      </Col>
    </Row>
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
