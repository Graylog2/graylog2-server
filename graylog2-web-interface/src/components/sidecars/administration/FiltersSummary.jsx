import React from 'react';
import PropTypes from 'prop-types';
import { Button, Col, Row } from 'react-bootstrap';
import lodash from 'lodash';

import SidecarStatusEnum from 'logic/sidecar/SidecarStatusEnum';

import style from './FiltersSummary.css';

class FiltersSummary extends React.Component {
  static propTypes = {
    collectors: PropTypes.array.isRequired,
    configurations: PropTypes.array.isRequired,
    filters: PropTypes.object.isRequired,
    onResetFilters: PropTypes.func.isRequired,
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
      return SidecarStatusEnum.toString(value);
    }

    return value;
  };

  formatFilters = (filters) => {
    return Object.keys(filters).map((filterKey) => {
      return <li key={filterKey}>{filterKey}: {this.formatFilter(filterKey, filters[filterKey])}</li>;
    });
  };

  render() {
    const { filters, onResetFilters } = this.props;

    if (lodash.isEmpty(filters)) {
      return null;
    }

    return (
      <Row className="row-sm">
        <Col md={10}>
          <ul className="list-inline">
            <li><b>Filters</b></li>
            {this.formatFilters(filters)}
            <li>
              <Button bsStyle="link" bsSize="xsmall" className={style.deleteButton} onClick={onResetFilters}>
                <i className="fa fa-times" /> Clear all</Button>
            </li>
          </ul>
        </Col>
      </Row>
    );
  }
}

export default FiltersSummary;
