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
import createReactClass from 'create-react-class';
import Reflux from 'reflux';

import { Row, Col } from 'components/graylog';
import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import StoreProvider from 'injection/StoreProvider';
import DateTime from 'logic/datetimes/DateTime';
import withParams from 'routing/withParams';

const NodesStore = StoreProvider.getStore('Nodes');
const CurrentUserStore = StoreProvider.getStore('CurrentUser');
const ClusterOverviewStore = StoreProvider.getStore('ClusterOverview');

function nodeFilter(state) {
  return state.nodes ? state.nodes[this.props.params.nodeId] : state.nodes;
}

const ProcessBufferDumpPage = createReactClass({
  displayName: 'ProcessBufferDumpPage',

  propTypes: {
    params: PropTypes.object.isRequired,
  },

  mixins: [Reflux.connect(CurrentUserStore), Reflux.connectFilter(NodesStore, 'node', nodeFilter)],

  componentDidMount() {
    const { params } = this.props;

    ClusterOverviewStore.processbufferDump(params.nodeId)
      .then((processbufferDump) => this.setState({ processbufferDump: processbufferDump }));
  },

  _isLoading() {
    return !this.state.node;
  },

  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }

    const { node, processbufferDump } = this.state;

    const title = (
      <span>
        Process-buffer dump of node {node.short_node_id} / {node.hostname}
        &nbsp;
        <small>Taken at {DateTime.now().toString(DateTime.Formats.COMPLETE)}</small>
      </span>
    );

    const content = processbufferDump ? <pre className="processbufferdump">{JSON.stringify(processbufferDump, null, 2)}</pre> : <Spinner />;

    return (
      <DocumentTitle title={`Process-buffer dump of node ${node.short_node_id} / ${node.hostname}`}>
        <div>
          <PageHeader title={title}>
            <span />
          </PageHeader>
          <Row className="content">
            <Col md={12}>
              {content}
            </Col>
          </Row>
        </div>
      </DocumentTitle>
    );
  },
});

export default withParams(ProcessBufferDumpPage);
