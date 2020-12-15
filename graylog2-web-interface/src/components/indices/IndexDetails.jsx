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

import { Col, Row, Button } from 'components/graylog';
import { Spinner } from 'components/common';
import ActionsProvider from 'injection/ActionsProvider';
import StoreProvider from 'injection/StoreProvider'; // To make IndexRangesActions work.
import { IndexRangeSummary, ShardMeter, ShardRoutingOverview } from 'components/indices';

const IndicesActions = ActionsProvider.getActions('Indices');
const IndexRangesActions = ActionsProvider.getActions('IndexRanges');

StoreProvider.getStore('IndexRanges');

class IndexDetails extends React.Component {
  static propTypes = {
    index: PropTypes.object.isRequired,
    indexName: PropTypes.string.isRequired,
    indexRange: PropTypes.object.isRequired,
    indexSetId: PropTypes.string.isRequired,
    isDeflector: PropTypes.bool.isRequired,
  };

  componentDidMount() {
    IndicesActions.subscribe(this.props.indexName);
  }

  componentWillUnmount() {
    IndicesActions.unsubscribe(this.props.indexName);
  }

  _formatActionButtons = () => {
    if (this.props.isDeflector) {
      return (
        <span>
          <Button bsStyle="warning" bsSize="xs" disabled>Active write index cannot be closed</Button>{' '}
          <Button bsStyle="danger" bsSize="xs" disabled>Active write index cannot be deleted</Button>
        </span>
      );
    }

    return (
      <span>
        <Button bsStyle="warning" bsSize="xs" onClick={this._onRecalculateIndex}>Recalculate index ranges</Button>{' '}
        <Button bsStyle="warning" bsSize="xs" onClick={this._onCloseIndex}>Close index</Button>{' '}
        <Button bsStyle="danger" bsSize="xs" onClick={this._onDeleteIndex}>Delete index</Button>
      </span>
    );
  };

  _onRecalculateIndex = () => {
    if (window.confirm(`Really recalculate the index ranges for index ${this.props.indexName}?`)) {
      IndexRangesActions.recalculateIndex(this.props.indexName).then(() => {
        IndicesActions.list(this.props.indexSetId);
      });
    }
  };

  _onCloseIndex = () => {
    if (window.confirm(`Really close index ${this.props.indexName}?`)) {
      IndicesActions.close(this.props.indexName).then(() => {
        IndicesActions.list(this.props.indexSetId);
      });
    }
  };

  _onDeleteIndex = () => {
    if (window.confirm(`Really delete index ${this.props.indexName}?`)) {
      IndicesActions.delete(this.props.indexName).then(() => {
        IndicesActions.list(this.props.indexSetId);
      });
    }
  };

  render() {
    if (!this.props.index || !this.props.index.all_shards) {
      return <Spinner />;
    }

    const { index, indexRange, indexName } = this.props;

    return (
      <div className="index-info">
        <IndexRangeSummary indexRange={indexRange} />{' '}

        {index.all_shards.segments} segments,{' '}
        {index.all_shards.open_search_contexts} open search contexts,{' '}
        {index.all_shards.documents.deleted} deleted messages

        <Row style={{ marginBottom: '10' }}>
          <Col md={4} className="shard-meters">
            <ShardMeter title="Primary shard operations" shardMeter={index.primary_shards} />
          </Col>
          <Col md={4} className="shard-meters">
            <ShardMeter title="Total shard operations" shardMeter={index.all_shards} />
          </Col>
        </Row>

        <ShardRoutingOverview routing={index.routing} indexName={indexName} />

        <hr style={{ marginBottom: '5', marginTop: '10' }} />

        {this._formatActionButtons()}
      </div>
    );
  }
}

export default IndexDetails;
