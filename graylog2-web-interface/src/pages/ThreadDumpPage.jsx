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

import { Row, Col } from 'components/bootstrap';
import { DocumentTitle, PageHeader, Spinner, Timestamp } from 'components/common';
import withParams from 'routing/withParams';
import { ClusterOverviewStore } from 'stores/cluster/ClusterOverviewStore';
import { CurrentUserStore } from 'stores/users/CurrentUserStore';
import { NodesStore } from 'stores/nodes/NodesStore';

function nodeFilter(state) {
  return state.nodes ? state.nodes[this.props.params.nodeId] : state.nodes;
}

const ThreadDumpPage = createReactClass({
  // eslint-disable-next-line react/no-unused-class-component-methods
  displayName: 'ThreadDumpPage',

  // eslint-disable-next-line react/no-unused-class-component-methods
  propTypes: {
    params: PropTypes.object.isRequired,
  },

  mixins: [Reflux.connect(CurrentUserStore), Reflux.connectFilter(NodesStore, 'node', nodeFilter)],

  componentDidMount() {
    ClusterOverviewStore.threadDump(this.props.params.nodeId).then((threadDump) => this.setState({ threadDump: threadDump }));
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
        Thread dump of node {this.state.node.short_node_id} / {this.state.node.hostname}
        &nbsp;
        <small>Taken at <Timestamp dateTime={new Date()} /></small>
      </span>
    );

    const threadDump = this.state.threadDump ? <pre className="threaddump">{this.state.threadDump}</pre> : <Spinner />;

    return (
      <DocumentTitle title={`Thread dump of node ${this.state.node.short_node_id} / ${this.state.node.hostname}`}>
        <div>
          <PageHeader title={title} />
          <Row className="content">
            <Col md={12}>
              {threadDump}
            </Col>
          </Row>
        </div>
      </DocumentTitle>
    );
  },
});

export default withParams(ThreadDumpPage);
