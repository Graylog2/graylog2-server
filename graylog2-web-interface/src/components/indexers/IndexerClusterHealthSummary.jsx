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
import PropTypes from 'prop-types';
import React from 'react';
import styled from 'styled-components';

import { Alert } from 'components/graylog';
import { Icon } from 'components/common';
import { DocumentationLink } from 'components/support';
import DocsHelper from 'util/DocsHelper';

const ESClusterStatus = styled(Alert)`
  margin-top: 10px;
  margin-bottom: 5px;
`;

class IndexerClusterHealthSummary extends React.Component {
  static propTypes = {
    health: PropTypes.object.isRequired,
  };

  _formatHealthStatus = ({ status }) => {
    return status.toLowerCase();
  };

  _alertClassForHealth = (health) => {
    switch (this._formatHealthStatus(health)) {
      case 'green': return 'success';
      case 'yellow': return 'warning';
      case 'red': return 'danger';
      default: return 'success';
    }
  };

  _formatTextForHealth = (health) => {
    const text = `Elasticsearch cluster is ${this._formatHealthStatus(health)}.`;

    switch (this._formatHealthStatus(health)) {
      case 'green': return text;
      case 'yellow':
      case 'red': return <strong>{text}</strong>;
      default: return text;
    }
  };

  _iconNameForHealth = (health) => {
    switch (this._formatHealthStatus(health)) {
      case 'green': return 'check-circle';
      case 'yellow': return 'exclamation-triangle';
      case 'red': return 'ambulance';
      default: return 'check-circle';
    }
  };

  render() {
    const { health } = this.props;

    return (
      <ESClusterStatus bsStyle={this._alertClassForHealth(health)}>
        <Icon name={this._iconNameForHealth(health)} /> &nbsp;{this._formatTextForHealth(health)}{' '}
        Shards:{' '}
        {health.shards.active} active,{' '}
        {health.shards.initializing} initializing,{' '}
        {health.shards.relocating} relocating,{' '}
        {health.shards.unassigned} unassigned,{' '}
        <DocumentationLink page={DocsHelper.PAGES.CLUSTER_STATUS_EXPLAINED} text="What does this mean?" />
      </ESClusterStatus>
    );
  }
}

export default IndexerClusterHealthSummary;
