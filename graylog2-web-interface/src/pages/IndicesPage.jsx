import React from 'react';
import Reflux from 'reflux';
import { Alert, Col, Row, Panel } from 'react-bootstrap';
import numeral from 'numeral';

import ActionsProvider from 'injection/ActionsProvider';
const IndexerOverviewActions = ActionsProvider.getActions('IndexerOverview');
const IndicesActions = ActionsProvider.getActions('Indices');

import StoreProvider from 'injection/StoreProvider';
const IndexerOverviewStore = StoreProvider.getStore('IndexerOverview');
const IndicesStore = StoreProvider.getStore('Indices');

import DocsHelper from 'util/DocsHelper';
import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import { DocumentationLink } from 'components/support';
import { IndexerClusterHealthSummary } from 'components/indexers';
import { IndicesMaintenanceDropdown, IndicesOverview } from 'components/indices';
import IndicesConfiguration from 'components/indices/IndicesConfiguration';

const IndicesPage = React.createClass({
  mixins: [
    Reflux.connect(IndicesStore, 'indexDetails'),
    Reflux.connect(IndexerOverviewStore),
  ],
  componentDidMount() {
    IndicesActions.list();
    this.timerId = setInterval(() => {
      IndicesActions.multiple();
      IndexerOverviewActions.list();
    }, this.REFRESH_INTERVAL);
  },
  componentWillUnmount() {
    if (this.timerId) {
      clearInterval(this.timerId);
    }
  },
  REFRESH_INTERVAL: 2000,
  _totalIndexCount() {
    return Object.keys(this.state.indexerOverview.indices).length;
  },
  _renderElasticsearchUnavailableInformation() {
    return (
      <Row className="content">
        <Col md={8} mdOffset={2}>
          <div className="top-margin">
            <Panel bsStyle="danger"
                   header={<span><i className="fa fa-exclamation-triangle" /> Indices overview unavailable</span>}>
              <p>
                We could not get the indices overview information. This usually means there was a problem
                connecting to Elasticsearch, and <strong>you should ensure Elasticsearch is up and reachable from
                Graylog</strong>.
              </p>
              <p>
                Graylog will continue storing your messages in its journal, but you will not be able to search on them
                until Elasticsearch is reachable again.
              </p>
            </Panel>
          </div>
        </Col>
      </Row>
    );
  },
  render() {
    const pageHeader = (
      <PageHeader title="Indices & Index Sets">
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
    );

    if (this.state.indexerOverviewError) {
      return (
        <span>
          {pageHeader}
          {this._renderElasticsearchUnavailableInformation()}
        </span>
      );
    }

    if (!this.state.indexerOverview || !this.state.indexDetails.closedIndices) {
      return <Spinner />;
    }

    const deflectorInfo = this.state.indexerOverview.deflector;
    return (
      <DocumentTitle title="Indices & Index Sets">
        <span>
          {pageHeader}

          <Row className="content">
            <Col md={12}>
              <IndicesConfiguration />
            </Col>
          </Row>

          <Row className="content">
            <Col md={12}>
              <Alert bsStyle="success" style={{ marginTop: '10' }}>
                <i className="fa fa-th"/> &nbsp;{this._totalIndexCount()} indices with a total of{' '}
                {numeral(this.state.indexerOverview.counts.events).format('0,0')} messages under management,
                current write-active index is <i>{deflectorInfo.current_target}</i>.
              </Alert>
              <IndexerClusterHealthSummary health={this.state.indexerOverview.indexer_cluster.health} />
            </Col>
          </Row>

          <IndicesOverview indices={this.state.indexerOverview.indices}
                           indexDetails={this.state.indexDetails.indices}
                           closedIndices={this.state.indexDetails.closedIndices}
                           deflector={this.state.indexerOverview.deflector}/>
        </span>
      </DocumentTitle>
    );
  },
});

export default IndicesPage;
