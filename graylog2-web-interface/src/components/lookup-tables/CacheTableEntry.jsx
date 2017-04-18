import React from 'react';
import { LinkContainer } from 'react-router-bootstrap';

import Routes from 'routing/Routes';

const LUTTableEntry = React.createClass({

  propTypes: {
    cache: React.PropTypes.object.isRequired,
  },

  render() {
    return (
      <tbody>
        <tr>
          <td>
            <LinkContainer to={Routes.SYSTEM.LOOKUPTABLES.CACHES.show(this.props.cache.name)}><a>{this.props.cache.title}</a></LinkContainer>
          </td>
          <td>{this.props.cache.description}</td>
          <td>{this.props.cache.name}</td>
          <td>TODO: <em>0</em></td>
          <td>TODO: <em>0 %</em></td>
        </tr>
      </tbody>
    );
  },

});

export default LUTTableEntry;

