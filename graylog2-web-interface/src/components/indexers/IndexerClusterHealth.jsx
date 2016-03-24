import React from 'react';
import Reflux from 'reflux';
import { Row, Col } from 'react-bootstrap';

import StoreProvider from 'injection/StoreProvider';
const IndexerClusterStore = StoreProvider.getStore('IndexerCluster');

import { Spinner } from 'components/common';
import { DocumentationLink, SmallSupportLink } from 'components/support';
import DocsHelper from 'util/DocsHelper';
import { IndexerClusterHealthSummary } from 'components/indexers';

const IndexerClusterHealth = React.createClass({
  mixins: [Reflux.connect(IndexerClusterStore)],
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

          <IndexerClusterHealthSummary health={health} />
        </Col>
      </Row>
    );
  },
});

export default IndexerClusterHealth;
