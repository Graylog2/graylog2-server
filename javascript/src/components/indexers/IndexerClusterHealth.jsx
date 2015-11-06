import React from 'react';
import Reflux from 'reflux';
import { Alert, Row, Col } from 'react-bootstrap';

import IndexerClusterStore from 'stores/indexers/IndexerClusterStore';

import { Spinner } from 'components/common';
import { DocumentationLink, SmallSupportLink } from 'components/support'
import DocsHelper from 'util/DocsHelper';

const IndexerClusterHealth = React.createClass({
  mixins: [Reflux.connect(IndexerClusterStore)],
  _alertClassForHealth(health) {
    switch (health.status) {
    case 'green': return 'success';
    case 'yellow': return 'warning';
    case 'red': return 'danger';
    default: return 'success';
    }
  },
  _formatTextForHealth(health) {
    const text = 'Elasticsearch cluster is ' + health.status + '.';
    switch (health.status) {
    case 'green': return text;
    case 'yellow':
    case 'red': return <strong>{text}</strong>;
    default: return text;
    }
  },
  _iconNameForHealth(health) {
    switch (health.status) {
    case 'green': return 'check-circle';
    case 'yellow': return 'warning';
    case 'red': return 'ambulance';
    default: return 'check-circle';
    }
  },
  render() {
    if (!this.state.health) {
      return <Spinner />;
    }
    const health = this.state.health;
    return (
      <Row className="content">
        <Col md={12}>
          <h2><i className="fa fa-tasks"></i> Elasticsearch cluster</h2>

          <SmallSupportLink>
            The possible Elasticsearch cluster states and more related information is available in the{' '}
            <DocumentationLink page={DocsHelper.PAGES.CONFIGURING_ES} text="Graylog documentation"/>.
          </SmallSupportLink>

          <Alert bsStyle={this._alertClassForHealth(health)} className="es-cluster-status">
            <i className={'fa fa-' + this._iconNameForHealth(health)}/> &nbsp;{this._formatTextForHealth(health)}{' '}
            Shards:{' '}
            {health.shards.active} active,{' '}
            {health.shards.initializing} initializing,{' '}
            {health.shards.relocating} relocating,{' '}
            {health.shards.unassigned} unassigned,{' '}
            <DocumentationLink page={DocsHelper.PAGES.CLUSTER_STATUS_EXPLAINED} text="What does this mean?"/>
          </Alert>
        </Col>
      </Row>
    );
  },
});

export default IndexerClusterHealth;
