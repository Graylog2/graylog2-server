import React from 'react';
import Reflux from 'reflux';
import { Row, Col } from 'react-bootstrap';
import { PageHeader, Spinner } from 'components/common';
import PluginList from './PluginList';

import StoreProvider from 'injection/StoreProvider';
import ActionsProvider from 'injection/ActionsProvider';

const NodesStore = StoreProvider.getStore('Nodes');
const NodesActions = ActionsProvider.getActions('Nodes');

const EnterprisePage = React.createClass({
  mixins: [Reflux.connect(NodesStore)],

  componentDidMount() {
    NodesActions.list();
  },

  _clusterId() {
    if (this.state.nodes) {
      return Object.keys(this.state.nodes).map(id => this.state.nodes[id]).map(node => node.cluster_id)[0].toUpperCase();
    }
    return null;
  },

  _numberOfNodes() {
    if (this.state.nodes) {
      return Object.keys(this.state.nodes).length;
    }

    return null;
  },

  render() {
    let orderLink = 'https://www.graylog.org/enterprise';
    let numberOfNodes = <Spinner />;
    let clusterId = <Spinner />;

    if (this.state.nodes) {
      orderLink = `https://www.graylog.org/enterprise?cid=${this._clusterId()}&nodes=${this._numberOfNodes()}`;
      numberOfNodes = this._numberOfNodes();
      clusterId = this._clusterId();
    }

    return (
      <div>
        <PageHeader title="Graylog Enterprise">
          {null}

          <span>
            Graylog Enterprise adds commercial functionality to the Open Source Graylog core. You can learn more
            about Graylog Enterprise and order a license on the <a href={orderLink} target="_blank">product page</a>.
          </span>

          <span>
            <a className="btn btn-lg btn-success" href={orderLink} target="_blank">Order a license</a>
          </span>
        </PageHeader>

        <Row className="content">
          <Col md={12}>
            <h2 style={{ marginBottom: 10 }}>Cluster Details</h2>
            <dl style={{ marginBottom: 0 }}>
              <dt>Cluster ID</dt>
              <dd>{clusterId}</dd>
              <dt>Number of nodes</dt>
              <dd>{numberOfNodes}</dd>
            </dl>
          </Col>
        </Row>

        <PluginList/>
      </div>
    );
  },
});

export default EnterprisePage;
