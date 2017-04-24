import React from 'react';
import { LinkContainer } from 'react-router-bootstrap';

import { Button } from 'react-bootstrap';

import Routes from 'routing/Routes';
import CombinedProvider from 'injection/CombinedProvider';

const { LookupTableCachesActions } = CombinedProvider.get('LookupTableCaches');

const LUTTableEntry = React.createClass({

  propTypes: {
    cache: React.PropTypes.object.isRequired,
    refresh: React.PropTypes.func.isRequired,
  },

  _onDelete() {
// eslint-disable-next-line no-alert
    if (window.confirm(`Are you sure you want to delete data adapter "${this.props.cache.title}"?`)) {
      LookupTableCachesActions.delete(this.props.cache.id).then(() => this.props.refresh());
    }
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
          <td>
            <LinkContainer to={Routes.SYSTEM.LOOKUPTABLES.DATA_ADAPTERS.edit(this.props.adapter.name)}>
              <Button bsSize="xsmall" bsStyle="info">Edit</Button>
            </LinkContainer>
            &nbsp;
            <Button bsSize="xsmall" bsStyle="primary" onClick={this._onDelete}>Delete</Button>
          </td>
        </tr>
      </tbody>
    );
  },

});

export default LUTTableEntry;

