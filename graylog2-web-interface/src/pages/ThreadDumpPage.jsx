import React, {PropTypes} from 'react';
import Reflux from 'reflux';
import { Row, Col } from 'react-bootstrap';

import { PageHeader, Spinner } from 'components/common';

import CurrentUserStore from 'stores/users/CurrentUserStore';
import NodesStore from 'stores/nodes/NodesStore';
import SystemThreadDumpStore from 'stores/cluster/SystemThreadDumpStore';

import DateTime from 'logic/datetimes/DateTime';

function nodeFilter(state) {
  return state.nodes ? state.nodes[this.props.params.nodeId] : state.nodes;
}

const ThreadDumpPage = React.createClass({
  propTypes: {
    params: PropTypes.object.isRequired,
  },
  mixins: [Reflux.connect(CurrentUserStore), Reflux.connectFilter(NodesStore, 'node', nodeFilter)],
  componentDidMount() {
    SystemThreadDumpStore.get(this.props.params.nodeId).then(threadDump => this.setState({threadDump: threadDump}));
  },
  _isLoading() {
    return !this.state.node;
  },
  render() {
    if (this._isLoading()) {
      return <Spinner/>;
    }

    const title = (
      <span>
        Thread dump of node {this.state.node.short_node_id} / {this.state.node.hostname}
        &nbsp;
        <small>Taken at {DateTime.now().toString(DateTime.Formats.COMPLETE)}</small>
      </span>
    );

    const threadDump = this.state.threadDump ? <pre className="threaddump">{this.state.threadDump}</pre> : <Spinner/>;

    return (
      <div>
        <PageHeader title={title}/>
        <Row className="content input-list">
          <Col md={12}>
            {threadDump}
          </Col>
        </Row>
      </div>
    );
  },
});

export default ThreadDumpPage;
