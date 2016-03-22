import React from 'react';
import { Alert, Button } from 'react-bootstrap';

import ActionsProvider from 'injection/ActionsProvider';
const IndicesActions = ActionsProvider.getActions('Indices');

import { IndexRangeSummary } from 'components/indices';

const ClosedIndexDetails = React.createClass({
  propTypes: {
    indexName: React.PropTypes.string.isRequired,
    indexRange: React.PropTypes.object.isRequired,
  },
  render() {
    const { indexName, indexRange } = this.props;
    return (
      <div className="index-info">
        <IndexRangeSummary indexRange={indexRange} />
        <Alert bsStyle="info"><i className="fa fa-info-circle"/> This index is closed. Index information is not available{' '}
          at the moment, please reopen the index and try again.</Alert>

        <hr style={{marginBottom: '5', marginTop: '10'}}/>

        <Button bsStyle="warning" bsSize="xs" onClick={() => { IndicesActions.reopen(indexName)}}>Reopen index</Button>{' '}
      </div>
    );
  },
});

export default ClosedIndexDetails;
