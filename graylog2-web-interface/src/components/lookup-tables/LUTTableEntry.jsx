import React from 'react';
import { LinkContainer } from 'react-router-bootstrap';

import Routes from 'routing/Routes';

const LUTTableEntry = React.createClass({

  propTypes: {
    table: React.PropTypes.object.isRequired,
    cache: React.PropTypes.object.isRequired,
    dataAdapter: React.PropTypes.object.isRequired,
  },

  render() {
    return (<tbody>
      <tr>
        <td>
          <LinkContainer to={Routes.SYSTEM.LOOKUPTABLES.show(this.props.table.name)}><a>{this.props.table.title}</a></LinkContainer>
        </td>
        <td>{this.props.table.description}</td>
        <td>{this.props.table.name}</td>
        <td>
          <LinkContainer to={Routes.SYSTEM.LOOKUPTABLES.CACHES.show(this.props.cache.name)}><a>{this.props.cache.title}</a></LinkContainer>
        </td>
        <td>
          <LinkContainer to={Routes.SYSTEM.LOOKUPTABLES.DATA_ADAPTERS.show(this.props.dataAdapter.name)}><a>{this.props.dataAdapter.title}</a></LinkContainer>
        </td>
      </tr>
    </tbody>);
  },

});

export default LUTTableEntry;

