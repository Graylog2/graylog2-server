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
import isEmpty from 'lodash/isEmpty';
import { withTranslation } from 'react-i18next';
import type { WithTranslation } from 'react-i18next';

import { Button, Col, Row } from 'components/bootstrap';
import { Icon } from 'components/common';
import SidecarStatusEnum from 'logic/sidecar/SidecarStatusEnum';

import style from './FiltersSummary.css';

type FiltersSummaryProps = {
  collectors: any[];
  configurations: any[];
  filters: any;
  onResetFilters: (...args: any[]) => void;
} & WithTranslation;

class FiltersSummary extends React.Component<
  FiltersSummaryProps,
  {
    [key: string]: any;
  }
> {
  formatFilter = (type, value) => {
    const { collectors, configurations } = this.props;

    if (type === 'collector') {
      // Get collector name
      const collector = collectors.find((c) => c.id === value);

      return this.props.t('administration.filters_summary.collector_on_os', {
        name: collector.name,
        os: collector.node_operating_system,
      });
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

  formatFilters = (filters) =>
    Object.keys(filters).map((filterKey) => (
      <li key={filterKey}>
        {filterKey}: {this.formatFilter(filterKey, filters[filterKey])}
      </li>
    ));

  render() {
    const { filters, onResetFilters } = this.props;

    if (isEmpty(filters)) {
      return null;
    }

    return (
      <Row className="row-sm">
        <Col md={10}>
          <ul className="list-inline">
            <li>
              <b>{this.props.t('administration.filters_summary.filters')}</b>
            </li>
            {this.formatFilters(filters)}
            <li>
              <Button bsStyle="link" bsSize="xsmall" className={style.deleteButton} onClick={onResetFilters}>
                <Icon name="close" /> {this.props.t('administration.filters_summary.clear_all')}
              </Button>
            </li>
          </ul>
        </Col>
      </Row>
    );
  }
}

export default withTranslation('sidecars')(FiltersSummary);
