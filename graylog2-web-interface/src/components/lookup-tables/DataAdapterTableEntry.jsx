import React from 'react';
import { LinkContainer } from 'react-router-bootstrap';

import { Button } from 'react-bootstrap';

import Routes from 'routing/Routes';
import CombinedProvider from 'injection/CombinedProvider';

const { LookupTableDataAdaptersActions } = CombinedProvider.get('LookupTableDataAdapters');

const DataAdapterTableEntry = React.createClass({

  propTypes: {
    adapter: React.PropTypes.object.isRequired,
  },

  _onDelete() {
// eslint-disable-next-line no-alert
    if (window.confirm(`Are you sure you want to delete data adapter "${this.props.adapter.title}"?`)) {
      LookupTableDataAdaptersActions.delete(this.props.adapter.id).then(() => LookupTableDataAdaptersActions.reloadPage());
    }
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

export default DataAdapterTableEntry;

