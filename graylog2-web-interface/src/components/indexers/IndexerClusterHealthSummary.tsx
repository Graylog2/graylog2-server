import React, { useMemo } from 'react';
import styled from 'styled-components';

import { Alert } from 'components/bootstrap';
import { DocumentationLink } from 'components/support';
import DocsHelper from 'util/DocsHelper';

const ESClusterStatus = styled(Alert)`
  margin-top: 10px;
  margin-bottom: 5px;
`;

const IndexerClusterHealthSummary = ({ health, name }: {
  health: {
    status: string,
    shards: {
      active: string,
      initializing: string,
      relocating: string,
      unassigned: string
    }
  },
  name?: {
    name: string,
    distribution: string,
  }
}) => {
  const formattedHealthStatus = health.status.toLowerCase();

  const alertClassForHealth = () => {
    switch (formattedHealthStatus) {
      case 'green': return 'success';
      case 'yellow': return 'warning';
      case 'red': return 'danger';
      default: return 'success';
    }
  };

  const formattedTextForHealth = useMemo(() => {
    const text = `${name?.distribution || 'Elasticsearch'} cluster ${name?.name || ''} is ${formattedHealthStatus}.`;

    switch (formattedHealthStatus) {
      case 'green': return text;
      case 'yellow':
      case 'red': return <strong>{text}</strong>;
      default: return text;
    }
  }, [formattedHealthStatus, name]);

  return (
    <ESClusterStatus bsStyle={alertClassForHealth()}>
      {formattedTextForHealth}{' '}
      Shards:{' '}
      {health.shards.active} active,{' '}
      {health.shards.initializing} initializing,{' '}
      {health.shards.relocating} relocating,{' '}
      {health.shards.unassigned} unassigned,{' '}
      <DocumentationLink page={DocsHelper.PAGES.CLUSTER_STATUS_EXPLAINED} text="What does this mean?" />
    </ESClusterStatus>
  );
};

export default IndexerClusterHealthSummary;
