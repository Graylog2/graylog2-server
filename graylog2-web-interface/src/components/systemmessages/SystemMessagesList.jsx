import PropTypes from 'prop-types';
import React from 'react';
import { Table } from 'react-bootstrap';

import { SystemMessage } from 'components/systemmessages';

class SystemMessagesList extends React.Component {
  static propTypes = {
    messages: PropTypes.arrayOf(PropTypes.object).isRequired,
  };

  render() {
    return (
      <Table className="system-messages" striped hover condensed>
        <thead>
          <tr>
            <th style={{ width: '200px' }}>Timestamp</th>
            <th>Node</th>
            <th>Message</th>
          </tr>
        </thead>

        <tbody>
          {this.props.messages.map(message => <SystemMessage key={`message-${Math.random().toString(36).substring(7)}`} message={message} />)}
        </tbody>
      </Table>
    );
  }
}

export default SystemMessagesList;
