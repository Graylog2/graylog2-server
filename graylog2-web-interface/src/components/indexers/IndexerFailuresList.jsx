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

import { Alert, Table } from 'components/graylog';
import { Icon } from 'components/common';
import { IndexerFailure } from 'components/indexers';

class IndexerFailuresList extends React.Component {
  static propTypes = {
    failures: PropTypes.arrayOf(PropTypes.object).isRequired,
  };

  render() {
    if (this.props.failures.length === 0) {
      return (
        <Alert bsStyle="success"><Icon name="check-circle" /> Hurray! There are not any indexer failures.</Alert>
      );
    }

    return (
      <div className="scrollable-table">
        <Table className="indexer-failures" striped hover condensed>
          <thead>
            <tr>
              <th style={{ width: 200 }}>Timestamp</th>
              <th>Index</th>
              <th>Letter ID</th>
              <th>Error message</th>
            </tr>
          </thead>
          <tbody>
            {this.props.failures.map((failure) => <IndexerFailure key={`indexer-failure-${failure.letter_id}`} failure={failure} />)}
          </tbody>
        </Table>
      </div>
    );
  }
}

export default IndexerFailuresList;
