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

  _alertClassForHealth = (health) => {
    switch (health.status) {
      case 'green': return 'success';
      case 'yellow': return 'warning';
      case 'red': return 'danger';
      default: return 'success';
    }
  };

  _formatTextForHealth = (health) => {
    const text = `Elasticsearch cluster is ${health.status}.`;
    switch (health.status) {
      case 'green': return text;
      case 'yellow':
      case 'red': return <strong>{text}</strong>;
      default: return text;
    }
  };

  _iconNameForHealth = (health) => {
    switch (health.status) {
      case 'green': return 'check-circle';
      case 'yellow': return 'warning';
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
