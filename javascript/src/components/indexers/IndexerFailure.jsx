import React from 'react';
import moment from 'moment';

const IndexerFailure = React.createClass({
  propTypes: {
    failure: React.PropTypes.object.isRequired,
  },
  render() {
    const failure = this.props.failure;
    return (
      <tr>
        <td title={failure.timestamp}>{moment(failure.timestamp).fromNow()}</td>
        <td>{failure.index}</td>
        <td>{failure.letter_id}</td>
        <td>{failure.message}</td>
      </tr>
    );
  },
});

export default IndexerFailure;
