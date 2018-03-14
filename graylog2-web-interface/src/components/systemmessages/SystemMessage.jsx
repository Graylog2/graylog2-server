import PropTypes from 'prop-types';
import React from 'react';
import moment from 'moment';

import { LinkToNode } from 'components/common';

class SystemMessage extends React.Component {
  static propTypes = {
    message: PropTypes.object.isRequired,
  };

  render() {
    const message = this.props.message;
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
