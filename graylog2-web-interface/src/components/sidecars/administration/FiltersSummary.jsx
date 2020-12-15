/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import React from 'react';
import PropTypes from 'prop-types';
import lodash from 'lodash';

import { Button, Col, Row } from 'components/graylog';
import { Icon } from 'components/common';
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
      const collector = collectors.find((c) => c.id === value);

      return `${collector.name} on ${collector.node_operating_system}`;
    }

    if (type === 'configuration') {
      // Get configuration name
      return configurations.find((c) => c.id === value).name;
    }

    if (type === 'status') {
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
                <Icon name="times" /> Clear all
              </Button>
            </li>
          </ul>
        </Col>
      </Row>
    );
  }
}

export default FiltersSummary;
