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

const ThreadDumpPage = createReactClass({
  displayName: 'ThreadDumpPage',

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
        <small>Taken at {DateTime.now().toString(DateTime.Formats.COMPLETE)}</small>
      </span>
    );

    const threadDump = this.state.threadDump ? <pre className="threaddump">{this.state.threadDump}</pre> : <Spinner />;

    return (
      <DocumentTitle title={`Thread dump of node ${this.state.node.short_node_id} / ${this.state.node.hostname}`}>
        <div>
          <PageHeader title={title}>
            <span />
          </PageHeader>
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

export default ThreadDumpPage;
