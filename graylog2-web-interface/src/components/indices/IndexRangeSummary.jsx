import React from 'react';
import moment from 'moment';

const IndexRangeSummary = React.createClass({
  propTypes: {
    indexRange: React.PropTypes.object.isRequired,
  },
  render() {
    const { indexRange } = this.props;
    if (!indexRange) {
      return <span><i>No index range available.</i></span>;
    }
    return (
      <span>Range re-calculated{' '}
        <span title={indexRange.calculated_at}>{moment(indexRange.calculated_at).fromNow()}</span>{' '}
        in {indexRange.took_ms}ms.
      </span>
    );
  },
});

export default IndexRangeSummary;
