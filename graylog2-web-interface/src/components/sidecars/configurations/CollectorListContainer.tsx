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

import { CollectorsActions, CollectorsStore } from 'stores/sidecars/CollectorsStore';
import { Spinner } from 'components/common';
import connect from 'stores/connect';
import withTelemetry from 'logic/telemetry/withTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import type { Collector } from 'components/sidecars/types';

import CollectorList from './CollectorList';

const validateCollector = (collector: Collector) => CollectorsActions.validate(collector);

const loadCollectors = () => {
  CollectorsActions.list({});
};

type CollectorListContainerProps = {
  collectors?: any;
  sendTelemetry?: (...args: any[]) => void;
};

class CollectorListContainer extends React.Component<CollectorListContainerProps, {
  [key: string]: any;
}> {
  static defaultProps = {
    collectors: undefined,
    sendTelemetry: () => {},
  };

  componentDidMount() {
    loadCollectors();
  }

  handleClone = (collector: string, name: string, callback: () => void) => {
    const { sendTelemetry } = this.props;

    sendTelemetry(TELEMETRY_EVENT_TYPE.SIDECARS.LOG_COLLECTOR_CLONED, {
      app_pathname: 'sidecars',
      app_section: 'configuration',
    });

    CollectorsActions.copy(collector, name)
      .then(() => {
        callback();
      });
  };

  handleDelete = async (collector) => {
    const { sendTelemetry } = this.props;

    sendTelemetry(TELEMETRY_EVENT_TYPE.SIDECARS.LOG_COLLECTOR_DELETED, {
      app_pathname: 'sidecars',
      app_section: 'configuration',
    });

    await CollectorsActions.delete(collector);
  };

  handlePageChange = (page: number, pageSize: number) => {
    const { query } = this.props.collectors;

    CollectorsActions.list({ query: query, page: page, pageSize: pageSize });
  };

  handleQueryChange = (query = '', callback = () => {}) => {
    const { pageSize } = this.props.collectors.pagination;

    CollectorsActions.list({ query: query, pageSize: pageSize }).finally(callback);
  };

  render() {
    const { collectors } = this.props;

    if (!collectors || !collectors.paginatedCollectors) {
      return <Spinner />;
    }

    return (
      <CollectorList collectors={collectors.paginatedCollectors}
                     pagination={collectors.pagination}
                     query={collectors.query}
                     total={collectors.total}
                     onPageChange={this.handlePageChange}
                     onQueryChange={this.handleQueryChange}
                     onClone={this.handleClone}
                     onDelete={this.handleDelete}
                     validateCollector={validateCollector} />
    );
  }
}

export default withTelemetry(connect(CollectorListContainer, { collectors: CollectorsStore }));
