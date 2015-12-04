import React from 'react';
import numeral from 'numeral';

const IndexSizeSummary = React.createClass({
  propTypes: {
    index: React.PropTypes.object.isRequired,
  },
  render() {
    const { index } = this.props;
    if (index.all_shards) {
      return (
        <span>({numeral(index.all_shards.store_size_bytes).format('0.0b')}
          / {numeral(index.all_shards.documents.count).format('0,0')} messages){' '}</span>
      );
    }

    return <span></span>;
  },
});

export default IndexSizeSummary;
