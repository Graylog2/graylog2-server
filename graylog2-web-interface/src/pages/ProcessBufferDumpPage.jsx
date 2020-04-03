import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import { Row, Col } from 'components/graylog';

import { DocumentTitle, PageHeader, Spinner } from 'components/common';

import StoreProvider from 'injection/StoreProvider';

import DateTime from 'logic/datetimes/DateTime';

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

export default ProcessBufferDumpPage;
