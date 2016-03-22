import React from 'react';
import Reflux from 'reflux';
import { Alert, Col, Row } from 'react-bootstrap';
import numeral from 'numeral';

import ActionsProvider from 'injection/ActionsProvider';
const IndexerClusterActions = ActionsProvider.getActions('IndexerCluster');
const DeflectorActions = ActionsProvider.getActions('Deflector');
const IndexRangesActions = ActionsProvider.getActions('IndexRanges');
const IndicesActions = ActionsProvider.getActions('Indices');
const MessageCountsActions = ActionsProvider.getActions('MessageCounts');

import IndexerClusterStore from 'stores/indexers/IndexerClusterStore';
import { DeflectorStore, IndexRangesStore, IndicesStore } from 'stores/indices';
import MessageCountsStore from 'stores/messages/MessageCountsStore';

import DocsHelper from 'util/DocsHelper';
import { PageHeader, Spinner } from 'components/common';
import { DocumentationLink } from 'components/support';
import { IndexerClusterHealthSummary } from 'components/indexers';
import { IndicesMaintenanceDropdown, IndicesOverview } from 'components/indices';
import IndicesConfiguration from 'components/indices/IndicesConfiguration';

const IndicesPage = React.createClass({
  mixins: [Reflux.connect(IndicesStore), Reflux.connect(DeflectorStore),
    Reflux.connect(IndexRangesStore), Reflux.connect(MessageCountsStore), Reflux.connect(IndexerClusterStore)],
  componentDidMount() {
    this.timerId = setInterval(() => {
      DeflectorActions.list();

      IndexRangesActions.list();

      IndicesActions.list();

      MessageCountsActions.total();

      IndexerClusterActions.health();
      IndexerClusterActions.name();
    }, this.REFRESH_INTERVAL);
  },
  componentWillUnmount() {
    if (this.timerId) {
      clearInterval(this.timerId);
    }
  },
  REFRESH_INTERVAL: 2000,
  _totalIndexCount() {
    return (Object.keys(this.state.indices).length + this.state.closedIndices.length);
  },
  render() {
    if (!this.state.indices || !this.state.indexRanges || !this.state.deflector || !this.state.health) {
      return <Spinner />;
    }
    const deflectorInfo = this.state.deflector.info;
    return (
      <span>
        <PageHeader title="Indices">
          <span>
            This is an overview of all indices (message stores) Graylog is currently taking in account
            for searches and analysis.
          </span>

          <span>
            You can learn more about the index model in the{' '}
            <DocumentationLink page={DocsHelper.PAGES.INDEX_MODEL} text="documentation" />
          </span>

          <IndicesMaintenanceDropdown />
        </PageHeader>

        <Row className="content">
          <Col md={12}>
            <IndicesConfiguration />
          </Col>
        </Row>

        <Row className="content">
          <Col md={12}>
            <Alert bsStyle="success" style={{marginTop: '10'}}>
              <i className="fa fa-th"/> &nbsp;{this._totalIndexCount()} indices with a total of{' '}
              {numeral(this.state.events).format('0,0')} messages under management,
              current write-active index is <i>{deflectorInfo ? deflectorInfo.current_target : <Spinner />}</i>.
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
