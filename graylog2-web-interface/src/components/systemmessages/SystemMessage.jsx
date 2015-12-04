import React from 'react';
import moment from 'moment';

const SystemMessage = React.createClass({
  propTypes: {
    message: React.PropTypes.object.isRequired,
  },
  render() {
    const message = this.props.message;
    return (
      <tr>
        <td>{moment(message.timestamp).format()}</td>
        <td>
          <i className="fa fa-code-fork"></i>
          // TODO: fix link to node
          <a href="@routes.NodesController.node(message.getNodeId)">
            {message.node_id}
          </a>
        </td>
        <td>{message.content}</td>
      </tr>
    );
  },
});

export default SystemMessage;
