import React from 'react';
import PropTypes from 'prop-types';
import { Col, Row } from 'react-bootstrap';
import lodash from 'lodash';

import StatusMapper from 'components/sidecars/common/StatusMapper';

class FiltersSummary extends React.Component {
  static propTypes = {
    collectors: PropTypes.array.isRequired,
    configurations: PropTypes.array.isRequired,
    filters: PropTypes.object.isRequired,
  };

  formatFilter = (type, value) => {
    const { collectors, configurations } = this.props;

    if (type === 'collector') {
      // Get collector name
      const collector = collectors.find(c => c.id === value);
      return `${collector.name} on ${collector.node_operating_system}`;
    } else if (type === 'configuration') {
      // Get configuration name
      return configurations.find(c => c.id === value).name;
    } else if (type === 'status') {
      // Convert status code to string
      return StatusMapper.toString(value);
    } else {
      return value;
    }
  };

  formatFilters = (filters) => {
    return Object.keys(filters).map((filterKey) => {
      return <li key={filterKey}>{filterKey}: {this.formatFilter(filterKey, filters[filterKey])}</li>;
    });
  };

  render() {
    const { filters } = this.props;

    if (lodash.isEmpty(filters)) {
      return null;
    }

    return (
      <Row className="row-sm">
        <Col md={10}>
          <ul className="list-inline">
            <li><b>Filters</b></li>
            {this.formatFilters(filters)}
          </ul>
        </Col>
      </Row>
    );
  }
}

export default FiltersSummary;
