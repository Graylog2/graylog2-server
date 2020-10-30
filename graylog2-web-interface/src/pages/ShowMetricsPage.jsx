import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';

import StoreProvider from 'injection/StoreProvider';
import ActionsProvider from 'injection/ActionsProvider';
import { DocumentTitle, Icon, PageHeader, Spinner } from 'components/common';
import { Alert, Col, Row } from 'components/graylog';
import { MetricsComponent } from 'components/metrics';
import withParams from 'routing/withParams';
import withLocation from 'routing/withLocation';

const NodesStore = StoreProvider.getStore('Nodes');
const MetricsStore = StoreProvider.getStore('Metrics');
const MetricsActions = ActionsProvider.getActions('Metrics');

const ShowMetricsPage = createReactClass({
  displayName: 'ShowMetricsPage',

  propTypes: {
    location: PropTypes.object.isRequired,
    params: PropTypes.object.isRequired,
  },

  mixins: [Reflux.connect(NodesStore), Reflux.connect(MetricsStore), Reflux.listenTo(NodesStore, '_getMetrics')],

  _getMetrics() {
    MetricsActions.names();
  },

  render() {
    if (!this.state.nodes || !this.state.metricsNames) {
      return <Spinner />;
    }

    let { nodeId } = this.props.params;

    // "master" node ID is a placeholder for master node, get first master node ID
    if (nodeId === 'master') {
      const nodeIDs = Object.keys(this.state.nodes);
      const masterNodes = nodeIDs.filter((nodeID) => this.state.nodes[nodeID].is_master);

      nodeId = masterNodes[0] || nodeIDs[0];
    }

    const node = this.state.nodes[nodeId];
    const title = <span>Metrics of node {node.short_node_id} / {node.hostname}</span>;
    const { namespace } = MetricsStore;
    const names = this.state.metricsNames[nodeId];
    const { filter } = this.props.location.query;

    return (
      <DocumentTitle title={`Metrics of node ${node.short_node_id} / ${node.hostname}`}>
        <span>
          <PageHeader title={title}>
            <span>
              All Graylog nodes provide a set of internal metrics for diagnosis, debugging and monitoring. Note that you can access
              all metrics via JMX, too.
            </span>
            {names ? (
              <span>This node is reporting a total of {names.length} metrics.</span>
            ) : (
              <span>Could not fetch metrics for this node.</span>
            )}

          </PageHeader>

          {names ? (
            <MetricsComponent names={names} namespace={namespace} nodeId={nodeId} filter={filter} />
          ) : (
            <Row className="content">
              <Col md={12}>
                <Alert bsStyle="danger">
                  <Icon name="exclamation-triangle" />&nbsp;
                  There was a problem fetching node metrics. Graylog will keep trying to get them in the background.
                </Alert>
              </Col>
            </Row>
          )}
        </span>
      </DocumentTitle>
    );
  },
});

export default withParams(withLocation(ShowMetricsPage));
