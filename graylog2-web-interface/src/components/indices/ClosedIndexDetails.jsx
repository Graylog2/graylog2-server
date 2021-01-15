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

import ActionsProvider from 'injection/ActionsProvider';
import { Alert, Button } from 'components/graylog';
import { Icon } from 'components/common';
import { IndexRangeSummary } from 'components/indices';

const IndicesActions = ActionsProvider.getActions('Indices');

class ClosedIndexDetails extends React.Component {
  static propTypes = {
    indexName: PropTypes.string.isRequired,
    indexRange: PropTypes.object,
  };

  _onReopen = () => {
    IndicesActions.reopen(this.props.indexName);
  };

  _onDeleteIndex = () => {
    if (window.confirm(`Really delete index ${this.props.indexName}?`)) {
      IndicesActions.delete(this.props.indexName);
    }
  };

  render() {
    const { indexRange } = this.props;

    return (
      <div className="index-info">
        <IndexRangeSummary indexRange={indexRange} />
        <Alert bsStyle="info"><Icon name="info-circle" /> This index is closed. Index information is not available{' '}
          at the moment, please reopen the index and try again.
        </Alert>

        <hr style={{ marginBottom: '5', marginTop: '10' }} />

        <Button bsStyle="warning" bsSize="xs" onClick={this._onReopen}>Reopen index</Button>{' '}
        <Button bsStyle="danger" bsSize="xs" onClick={this._onDeleteIndex}>Delete index</Button>
      </div>
    );
  }
}

export default ClosedIndexDetails;
