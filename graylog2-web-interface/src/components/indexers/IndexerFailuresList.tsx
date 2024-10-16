import React from 'react';

import { Alert, Table } from 'components/bootstrap';
import { Icon } from 'components/common';
import { IndexerFailure } from 'components/indexers';

type IndexerFailuresListProps = {
  failures: any[];
};

class IndexerFailuresList extends React.Component<IndexerFailuresListProps, {
  [key: string]: any;
}> {
  render() {
    if (this.props.failures.length === 0) {
      return (
        <Alert bsStyle="success"><Icon name="check_circle" /> Hurray! There are not any indexer failures.</Alert>
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
