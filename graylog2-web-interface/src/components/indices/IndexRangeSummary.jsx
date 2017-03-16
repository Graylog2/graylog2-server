import React from 'react';
import { Timestamp } from 'components/common';

const IndexRangeSummary = React.createClass({
  propTypes: {
    indexRange: React.PropTypes.object,
  },
  render() {
    const { indexRange } = this.props;
    if (!indexRange) {
      return <span><i>No index range available.</i></span>;
    }
    return (
      <span>Range re-calculated{' '}
        <span title={indexRange.calculated_at}><Timestamp dateTime={indexRange.calculated_at} relative /></span>{' '}
        in {indexRange.took_ms}ms.
      </span>
    );
  },
});

export default IndexRangeSummary;
