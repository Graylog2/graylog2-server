import PropTypes from 'prop-types';
import React from 'react';
import { Timestamp } from 'components/common';

class IndexerFailure extends React.Component {
  static propTypes = {
    failure: PropTypes.object.isRequired,
  };

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
  }
}

export default IndexerFailure;
