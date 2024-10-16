import React from 'react';
import numeral from 'numeral';

import NumberUtils from 'util/NumberUtils';

type IndexSizeSummaryProps = {
  index: any;
};

class IndexSizeSummary extends React.Component<IndexSizeSummaryProps, {
  [key: string]: any;
}> {
  render() {
    const { index } = this.props;

    if (index.size) {
      return (
        <span>({NumberUtils.formatBytes(index.size.bytes)}{' '}
          / {numeral(index.size.events).format('0,0')} messages){' '}
        </span>
      );
    }

    return <span />;
  }
}

export default IndexSizeSummary;
