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
// eslint-disable-next-line no-restricted-imports
import createReactClass from 'create-react-class';
import Reflux from 'reflux';

import { Row, Col, Button } from 'components/bootstrap';
import { DocumentTitle, Icon, PageHeader, Spinner, Timestamp } from 'components/common';
import withParams from 'routing/withParams';
import withHistory from 'routing/withHistory';
import { ClusterOverviewStore } from 'stores/cluster/ClusterOverviewStore';
import { CurrentUserStore } from 'stores/users/CurrentUserStore';
import { NodesStore } from 'stores/nodes/NodesStore';
import Routes from 'routing/Routes';

function nodeFilter(state) {
  return state.nodes ? state.nodes[this.props.params.nodeId] : state.nodes;
}

const DEFAULT_LIMIT = 5000;

const SystemLogsPage = createReactClass({
  // eslint-disable-next-line react/no-unused-class-component-methods
  displayName: 'SystemLogsPage',

  // eslint-disable-next-line react/no-unused-class-component-methods
  propTypes: {
    params: PropTypes.object.isRequired,
    history: PropTypes.object.isRequired,
  },

  mixins: [Reflux.connect(CurrentUserStore), Reflux.connectFilter(NodesStore, 'node', nodeFilter)],

  componentDidMount() {
    this.fetchLogs();
  },

  fetchLogs() {
    this.setState({ isReloadingResults: true });
    ClusterOverviewStore.systemLogs(this.props.params.nodeId, DEFAULT_LIMIT).then((sysLogs) => this.setState({ sysLogs: sysLogs, taken: new Date() }));
    this.setState({ isReloadingResults: false });
  },

  cancel() {
    const { history } = this.props;
    history.push(Routes.SYSTEM.NODES.LIST);
  },

  _isLoading() {
    return !this.state.node;
  },

  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }

    const title = (
      <span>
        The most recent system logs (limited to {DEFAULT_LIMIT}) of node {this.state.node.short_node_id} / {this.state.node.hostname}
        &nbsp;
        <small>Taken at <Timestamp dateTime={this.state.taken} /></small>
      </span>
    );

    const control = (
      <Col md={1}>
        <small>Reload&nbsp;</small>
        <Button onClick={this.fetchLogs} disabled={this.state.isReloadingResults}>
          <Icon name="sync" spin={this.state.isReloadingResults} />
        </Button>
      </Col>
    );

    const backButton = (
      <Col md={1}>
        <Button onClick={this.cancel}>
          Back
        </Button>
      </Col>
    );

    const sysLogs = this.state.sysLogs ? <pre className="threaddump">{this.state.sysLogs}</pre> : <Spinner />;

    return (
      <DocumentTitle title={`System Logs of node ${this.state.node.short_node_id} / ${this.state.node.hostname}`}>
        <div>
          <PageHeader title={title} />
          <Row className="content">
            {control} {backButton}
          </Row>
          <Row className="content">
            <Col md={12}>
              {sysLogs}
            </Col>
          </Row>
          <Row className="content">
            {control}
          </Row>
        </div>
      </DocumentTitle>
    );
  },
});

export default withHistory(withParams(SystemLogsPage));
