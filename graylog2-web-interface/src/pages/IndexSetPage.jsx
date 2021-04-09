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
import numeral from 'numeral';

import HideOnCloud from 'util/conditional/HideOnCloud';
import { LinkContainer } from 'components/graylog/router';
import { Alert, Row, Col, Panel, Button } from 'components/graylog';
import { DocumentTitle, PageHeader, Spinner, Icon } from 'components/common';
import { IndicesMaintenanceDropdown, IndicesOverview, IndexSetDetails } from 'components/indices';
import { IndexerClusterHealthSummary } from 'components/indexers';
import { DocumentationLink } from 'components/support';
import DocsHelper from 'util/DocsHelper';
import CombinedProvider from 'injection/CombinedProvider';
import Routes from 'routing/Routes';
import withParams from 'routing/withParams';

const { IndexSetsStore, IndexSetsActions } = CombinedProvider.get('IndexSets');
const { IndicesStore, IndicesActions } = CombinedProvider.get('Indices');
const { IndexerOverviewStore, IndexerOverviewActions } = CombinedProvider.get('IndexerOverview');

const IndexSetPage = createReactClass({
  displayName: 'IndexSetPage',

  propTypes: {
    params: PropTypes.object.isRequired,
  },

  mixins: [
    Reflux.connect(IndexSetsStore),
    Reflux.connect(IndicesStore, 'indexDetails'),
    Reflux.connect(IndexerOverviewStore),
  ],

  getInitialState() {
    return {
      indexSet: undefined,
    };
  },

  componentDidMount() {
    IndexSetsActions.get(this.props.params.indexSetId);
    IndicesActions.list(this.props.params.indexSetId);

    this.timerId = setInterval(() => {
      IndicesActions.multiple();
      IndexerOverviewActions.list(this.props.params.indexSetId);
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
                   header={<span><Icon name="exclamation-triangle" /> Indices overview unavailable</span>}>
              <p>
                We could not get the indices overview information. This usually means there was a problem
                connecting to Elasticsearch, and <strong>you should ensure Elasticsearch is up and reachable from
                  Graylog
                                                 </strong>.
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

  _isLoading() {
    return !this.state.indexSet;
  },

  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }

    const { indexSet } = this.state;

    const pageHeader = (
      <PageHeader title={`Index Set: ${indexSet.title}`}>
        <span>
          This is an overview of all indices (message stores) in this index set Graylog is currently taking in account
          for searches and analysis.
        </span>

        <span>
          You can learn more about the index model in the{' '}
          <DocumentationLink page={DocsHelper.PAGES.INDEX_MODEL} text="documentation" />
        </span>

        <span>
          <LinkContainer to={Routes.SYSTEM.INDICES.LIST}>
            <Button bsStyle="info">Index sets overview</Button>
          </LinkContainer>
          &nbsp;
          <LinkContainer to={Routes.SYSTEM.INDEX_SETS.CONFIGURATION(indexSet.id, 'details')}>
            <Button bsStyle="info">Edit Index Set</Button>
          </LinkContainer>
          &nbsp;
          <IndicesMaintenanceDropdown indexSetId={this.props.params.indexSetId} indexSet={this.state.indexSet} />
        </span>
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

    let indicesInfo;
    let indicesOverview;

    if (this.state.indexerOverview && this.state.indexDetails.closedIndices) {
      const deflectorInfo = this.state.indexerOverview.deflector;

      indicesInfo = (
        <span>
          <Alert bsStyle="success" style={{ marginTop: '10' }}>
            <Icon name="th" /> &nbsp;{this._totalIndexCount()} indices with a total of{' '}
            {numeral(this.state.indexerOverview.counts.events).format('0,0')} messages under management,
            current write-active index is <i>{deflectorInfo.current_target}</i>.
          </Alert>
          <HideOnCloud>
            <IndexerClusterHealthSummary health={this.state.indexerOverview.indexer_cluster.health} />
          </HideOnCloud>
        </span>
      );

      indicesOverview = (
        <IndicesOverview indices={this.state.indexerOverview.indices}
                         indexDetails={this.state.indexDetails.indices}
                         indexSetId={this.props.params.indexSetId}
                         closedIndices={this.state.indexDetails.closedIndices}
                         deflector={this.state.indexerOverview.deflector} />
      );
    } else {
      indicesInfo = <Spinner />;
      indicesOverview = <Spinner />;
    }

    return (
      <DocumentTitle title={`Index Set - ${indexSet.title}`}>
        <div>
          {pageHeader}

          <Row className="content">
            <Col md={12}>
              <IndexSetDetails indexSet={indexSet} />
            </Col>
          </Row>

          <Row className="content">
            <Col md={12}>
              {indicesInfo}
            </Col>
          </Row>

          {indicesOverview}
        </div>
      </DocumentTitle>
    );
  },
});

export default withParams(IndexSetPage);
