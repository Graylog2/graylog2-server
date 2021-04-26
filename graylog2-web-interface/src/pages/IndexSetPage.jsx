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
// @flow strict
import React from 'react';
import PropTypes from 'prop-types';
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
import connect from 'stores/connect';
import type { IndexSet } from 'stores/indices/IndexSetsStore';
import type { IndexerOverview } from 'stores/indexers/IndexerOverviewStore';
import type { Indices } from 'stores/indices/IndicesStore';

const { IndexSetsStore, IndexSetsActions } = CombinedProvider.get('IndexSets');
const { IndicesStore, IndicesActions } = CombinedProvider.get('Indices');
const { IndexerOverviewStore, IndexerOverviewActions } = CombinedProvider.get('IndexerOverview');

const REFRESH_INTERVAL = 2000;

type Props = {
  params: {
    indexSetId: ?string,
  },
  indexSet: ?IndexSet,
  indexerOverview: ?IndexerOverview,
  indexerOverviewError: ?string,
  indexDetails: {
    closedIndices: ?Indices,
    indices: ?Indices,
  },
};

type State = {
  timerId: ?IntervalID,
};

class IndexSetPage extends React.Component<Props, State> {
  static propTypes = {
    params: PropTypes.shape({
      indexSetId: PropTypes.string,
    }).isRequired,
    indexSet: PropTypes.object,
    indexerOverview: PropTypes.object,
    indexerOverviewError: PropTypes.object,
    indexDetails: PropTypes.object,
  };

  static defaultProps = {
    indexerOverview: undefined,
    indexerOverviewError: undefined,
    indexSet: undefined,
    indexDetails: {
      indices: undefined,
      closedIndices: undefined,
    },
  }

  constructor(props) {
    super(props);

    this.state = {
      timerId: undefined,
    };
  }

  componentDidMount() {
    const { params: { indexSetId } } = this.props;
    IndexSetsActions.get(indexSetId);
    IndicesActions.list(indexSetId);

    const timerId = setInterval(() => {
      IndicesActions.multiple();
      IndexerOverviewActions.list(indexSetId);
    }, REFRESH_INTERVAL);
    this.setState({ timerId: timerId });
  }

  componentWillUnmount() {
    const { timerId } = this.state;

    if (timerId) {
      clearInterval(timerId);
    }
  }

  _totalIndexCount = () => {
    const { indexerOverview: indices } = this.props;

    return indices ? Object.keys(indices).length : null;
  };

  _renderElasticsearchUnavailableInformation = () => {
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
  };

  _isLoading = () => {
    const { indexSet } = this.props;

    return !indexSet;
  };

  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }

    const { indexSet, indexerOverview, indexerOverviewError, params: { indexSetId }, indexDetails: { indices, closedIndices } } = this.props;

    const pageHeader = indexSet && (
      <PageHeader title={`Index Set: ${indexSet?.title}`}>
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
          <LinkContainer to={Routes.SYSTEM.INDEX_SETS.CONFIGURATION(indexSet?.id, 'details')}>
            <Button bsStyle="info">Edit Index Set</Button>
          </LinkContainer>
          &nbsp;
          <IndicesMaintenanceDropdown indexSetId={indexSetId} indexSet={indexSet} />
        </span>
      </PageHeader>
    );

    if (indexerOverviewError) {
      return (
        <span>
          {pageHeader}
          {this._renderElasticsearchUnavailableInformation()}
        </span>
      );
    }

    let indicesInfo;
    let indicesOverview;

    if (indexerOverview && closedIndices) {
      const deflectorInfo = indexerOverview.deflector;

      indicesInfo = (
        <span>
          <Alert bsStyle="success" style={{ marginTop: '10' }}>
            <Icon name="th" /> &nbsp;{this._totalIndexCount()} indices with a total of{' '}
            {numeral(indexerOverview.counts.events).format('0,0')} messages under management,
            current write-active index is <i>{deflectorInfo.current_target}</i>.
          </Alert>
          <HideOnCloud>
            <IndexerClusterHealthSummary health={indexerOverview.indexer_cluster.health} />
          </HideOnCloud>
        </span>
      );

      indicesOverview = (
        <IndicesOverview indices={indexerOverview.indices}
                         indexDetails={indices}
                         indexSetId={indexSetId}
                         closedIndices={closedIndices}
                         deflector={indexerOverview.deflector} />
      );
    } else {
      indicesInfo = <Spinner />;
      indicesOverview = <Spinner />;
    }

    return (
      <DocumentTitle title={`Index Set - ${indexSet ? indexSet.title : ''}`}>
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
  }
}

export default connect(
  withParams(IndexSetPage),
  {
    indexSets: IndexSetsStore,
    indexerOverview: IndexerOverviewStore,
    indices: IndicesStore,
  },
  ({ indexSets, indexerOverview, indices }) => ({
    indexSet: indexSets ? indexSets.indexSet : undefined,
    indexerOverview: indexerOverview && indexerOverview.indexerOverview,
    indexerOverviewError: indexerOverview && indexerOverview.indexerOverviewError,
    indexDetails: indices,
  }),
);
