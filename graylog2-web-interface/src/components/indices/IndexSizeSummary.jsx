import React from 'react';
import numeral from 'numeral';

const IndexSizeSummary = React.createClass({
  propTypes: {
    index: React.PropTypes.object.isRequired,
  },
  render() {
    const { index } = this.props;
    if (index.size) {
      return (
        <span>({numeral(index.size.bytes).format('0.0b')}{' '}
          / {numeral(index.size.events).format('0,0')} messages){' '}</span>
      );
    }

    return <span></span>;
  },
});

export default IndexSizeSummary;
