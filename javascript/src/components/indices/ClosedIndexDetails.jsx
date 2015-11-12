import React from 'react';
import { Alert } from 'react-bootstrap';

import { IndexRangeSummary } from 'components/indices';

const ClosedIndexDetails = React.createClass({
  propTypes: {
    indexRange: React.PropTypes.object.isRequired,
  },
  render() {
    const { indexRange } = this.props;
    return (
      <div className="index-info">
        <IndexRangeSummary indexRange={indexRange} />
        <Alert bsStyle="info"><i className="fa fa-info-circle"/> This index is closed. Index information is not available{' '}
          at the moment, please reopen the index and try again.</Alert>
      </div>
    );
  },
});

export default ClosedIndexDetails;
