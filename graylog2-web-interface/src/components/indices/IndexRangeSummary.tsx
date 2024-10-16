import React from 'react';

import RelativeTime from 'components/common/RelativeTime';

type IndexRangeSummaryProps = {
  indexRange?: any;
};

class IndexRangeSummary extends React.Component<IndexRangeSummaryProps, {
  [key: string]: any;
}> {
  render() {
    const { indexRange } = this.props;

    if (!indexRange) {
      return <span><i>No index range available.</i></span>;
    }

    return (
      <span>Range re-calculated{' '}
        <RelativeTime dateTime={indexRange.calculated_at} />{' '}
        in {indexRange.took_ms}ms.
      </span>
    );
  }
}

export default IndexRangeSummary;
