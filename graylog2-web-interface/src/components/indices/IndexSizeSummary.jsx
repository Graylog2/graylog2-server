import React from 'react';
import numeral from 'numeral';
import NumberUtils from 'util/NumberUtils';

const IndexSizeSummary = React.createClass({
  propTypes: {
    index: React.PropTypes.object.isRequired,
  },
  render() {
    const { index } = this.props;
    if (index.size) {
      return (
        <span>({NumberUtils.formatBytes(index.size.bytes)}{' '}
          / {numeral(index.size.events).format('0,0')} messages){' '}</span>
      );
    }

    return <span />;
  },
});

export default IndexSizeSummary;
