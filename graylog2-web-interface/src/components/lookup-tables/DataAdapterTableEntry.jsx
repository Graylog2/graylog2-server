import React from 'react';
import { LinkContainer } from 'react-router-bootstrap';

import Routes from 'routing/Routes';

const DataAdapterTableEntry = React.createClass({

  propTypes: {
    adapter: React.PropTypes.object.isRequired,
  },

  render() {
    return (
      <tbody>
        <tr>
          <td>
            <LinkContainer to={Routes.SYSTEM.LOOKUPTABLES.DATA_ADAPTERS.show(this.props.adapter.name)}><a>{this.props.adapter.title}</a></LinkContainer>
          </td>
          <td>{this.props.adapter.description}</td>
          <td>{this.props.adapter.name}</td>
          <td>TODO: <em>0</em></td>
          <td>TODO: <em>0 %</em></td>
        </tr>
      </tbody>
    );
  },

});

export default DataAdapterTableEntry;

