import React from 'react';
import moment from 'moment';

import { LinkToNode } from 'components/common';

type SystemMessageProps = {
  message: any;
};

class SystemMessage extends React.Component<SystemMessageProps, {
  [key: string]: any;
}> {
  render() {
    const { message } = this.props;

    return (
      <tr>
        <td>{moment(message.timestamp).format()}</td>
        <td>
          <LinkToNode nodeId={message.node_id} />
        </td>
        <td>{message.content}</td>
      </tr>
    );
  }
}

export default SystemMessage;
