import React from 'react';
import { Timestamp } from 'components/common';

const IndexerFailure = React.createClass({
  propTypes: {
    failure: React.PropTypes.object.isRequired,
  },
  render() {
    const failure = this.props.failure;
    return (
      <tr>
        <td title={failure.timestamp}><Timestamp dateTime={failure.timestamp} relative /></td>
        <td>{failure.index}</td>
        <td>{failure.letter_id}</td>
        <td>{failure.message}</td>
      </tr>
    );
  },
});

export default IndexerFailure;
