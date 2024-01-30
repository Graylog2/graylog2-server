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

import { Spinner } from 'components/common';
import { CollectorsActions, CollectorsStore } from 'stores/sidecars/CollectorsStore';
import { CollectorConfigurationsActions, CollectorConfigurationsStore } from 'stores/sidecars/CollectorConfigurationsStore';
import withTelemetry from 'logic/telemetry/withTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import connect from 'stores/connect';

import ConfigurationList from './ConfigurationList';

const handleDelete = (configuration) => CollectorConfigurationsActions.delete(configuration);

const reloadConfiguration = () => {
  CollectorConfigurationsActions.list({});
  CollectorsActions.all();
};

const validateConfiguration = (configuration) => CollectorConfigurationsActions.validate(configuration);

class ConfigurationListContainer extends React.Component {
  componentDidMount() {
    reloadConfiguration();
  }

  handlePageChange = (page, pageSize) => {
    const { query } = this.props.configurations;

    CollectorConfigurationsActions.list({ query: query, page: page, pageSize: pageSize });
  };

  handleQueryChange = (query = '', callback = () => {}) => {
    const { pageSize } = this.props.configurations.pagination;

    CollectorConfigurationsActions.list({ query: query, pageSize: pageSize }).finally(callback);
  };

  handleClone = (configuration, name, callback) => {
    const { sendTelemetry } = this.props;

    sendTelemetry(TELEMETRY_EVENT_TYPE.SIDECARS.CONFIGURATION_CLONED, {
      app_pathname: 'sidecars',
      app_section: 'configuration',
    });

    CollectorConfigurationsActions.copyConfiguration(configuration, name)
      .then((response) => {
        callback();

        return response;
      });
  };

  render() {
    const { collectors, configurations } = this.props;
    const isLoading = !collectors || !configurations || !configurations.paginatedConfigurations;

    if (isLoading) {
      return <Spinner />;
    }

    return (
      <ConfigurationList collectors={collectors}
                         query={configurations.query}
                         pagination={configurations.pagination}
                         total={configurations.total}
                         configurations={configurations.paginatedConfigurations}
                         onPageChange={this.handlePageChange}
                         onQueryChange={this.handleQueryChange}
                         onClone={this.handleClone}
                         onDelete={handleDelete}
                         validateConfiguration={validateConfiguration} />
    );
  }
}

ConfigurationListContainer.propTypes = {
  configurations: PropTypes.object,
  collectors: PropTypes.array,
  sendTelemetry: PropTypes.func,
};

ConfigurationListContainer.defaultProps = {
  configurations: undefined,
  collectors: undefined,
  sendTelemetry: () => {},
};

export default withTelemetry(connect(ConfigurationListContainer, {
  configurations: CollectorConfigurationsStore,
  collectorsState: CollectorsStore,
}, ({ configurations, collectorsState }) => ({
  collectors: collectorsState.collectors,
  configurations,
})));
