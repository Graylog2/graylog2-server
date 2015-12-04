import React from 'react';
import Reflux from 'reflux';
import { Alert, Col, Row } from 'react-bootstrap';
import numeral from 'numeral';

import IndexerClusterActions from 'actions/indexers/IndexerClusterActions';
import { DeflectorActions, IndexRangesActions, IndicesActions } from 'actions/indices';
import MessageCountsActions from 'actions/messages/MessageCountsActions';

import IndexerClusterStore from 'stores/indexers/IndexerClusterStore';
import { DeflectorStore, IndexRangesStore, IndicesStore } from 'stores/indices';
import MessageCountsStore from 'stores/messages/MessageCountsStore';

import DocsHelper from 'util/DocsHelper';
import { PageHeader, Spinner } from 'components/common';
import { DocumentationLink } from 'components/support';
import { IndexerClusterHealthSummary } from 'components/indexers';
import { DeflectorConfigSummary, IndicesMaintenanceDropdown, IndicesOverview } from 'components/indices';

const IndicesPage = React.createClass({
  componentDidMount() {
    const timerId = setInterval(() => {
      DeflectorActions.config();
      DeflectorActions.list();

      IndexRangesActions.list();

      IndicesActions.list();

      MessageCountsActions.total();

      IndexerClusterActions
    }, this.REFRESH_INTERVAL);
    this.setState({timerId: timerId});
  },
  componentWillUnmount() {
    clearInterval(this.state.timerId);
  },
  mixins: [Reflux.connect(IndicesStore), Reflux.connect(DeflectorStore),
    Reflux.connect(IndexRangesStore), Reflux.connect(MessageCountsStore), Reflux.connect(IndexerClusterStore)],
  REFRESH_INTERVAL: 2000,
  _totalIndexCount() {
    return (Object.keys(this.state.indices).length + this.state.closedIndices.length);
  },
  render() {
    if (!this.state.indices || !this.state.indexRanges || !this.state.deflector || !this.state.health) {
      return <Spinner />;
    }
    return (
      <span>
        <PageHeader title="Indices">
          <span>
            This is an overview of all indices (message stores) Graylog is currently taking in account
            for searches and analysis. You can learn more about the index model in the{' '}
            <DocumentationLink page={DocsHelper.PAGES.INDEX_MODEL} text="documentation" />.
            Closed indices can be re-opened at any time.
          </span>

          <span>
            <DeflectorConfigSummary config={this.state.deflector.config} />
          </span>

          <IndicesMaintenanceDropdown />
        </PageHeader>

        <Row className="content">
          <Col md={12}>
            <Alert bsStyle="success" style={{marginTop: '10'}}>
              <i className="fa fa-th"/> &nbsp;{this._totalIndexCount()} indices with a total of{' '}
              {numeral(this.state.events).format('0,0')} messages under management,
              current write-active index is <i>{this.state.deflector.info.current_target}</i>.
            </Alert>
            <IndexerClusterHealthSummary health={this.state.health} />
          </Col>
        </Row>

        <IndicesOverview indices={this.state.indices} closedIndices={this.state.closedIndices}
                         deflector={this.state.deflector} indexRanges={this.state.indexRanges} />
      </span>
    );
  },
});

export default IndicesPage;
