import React from 'react';

import RelativeTime from 'components/common/RelativeTime';

type IndexerFailureProps = {
  failure: any;
};

class IndexerFailure extends React.Component<IndexerFailureProps, {
  [key: string]: any;
}> {
  render() {
    const { failure } = this.props;

    return (
      <tr>
        <td><RelativeTime dateTime={failure.timestamp} /></td>
        <td>{failure.index}</td>
        <td>{failure.letter_id}</td>
        <td>{failure.message}</td>
      </tr>
    );
  }
}

export default IndexerFailure;
