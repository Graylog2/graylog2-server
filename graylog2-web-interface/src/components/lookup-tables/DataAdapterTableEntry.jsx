import React from 'react';
import { LinkContainer } from 'react-router-bootstrap';

import { Button, OverlayTrigger, Popover } from 'react-bootstrap';

import Routes from 'routing/Routes';
import CombinedProvider from 'injection/CombinedProvider';

const { LookupTableDataAdaptersActions } = CombinedProvider.get('LookupTableDataAdapters');

const DataAdapterTableEntry = React.createClass({

  propTypes: {
    adapter: React.PropTypes.object.isRequired,
    error: React.PropTypes.string,
  },

  getDefaultProps() {
    return {
      error: null,
    };
  },

  _onDelete() {
// eslint-disable-next-line no-alert
    if (window.confirm(`Are you sure you want to delete data adapter "${this.props.adapter.title}"?`)) {
      LookupTableDataAdaptersActions.delete(this.props.adapter.id).then(() => LookupTableDataAdaptersActions.reloadPage());
    }
  },

  render() {
    const errorOverlay = !this.props.error ? null : (
      <Popover id="popover-table-error" title="Lookup Table problem" style={{ maxWidth: 400 }}>
        {this.props.error}
      </Popover>);
    return (
      <tbody>
        <tr>
          <td>
            {this.props.error && (
              <OverlayTrigger trigger={['hover', 'focus']} placement="right" overlay={errorOverlay}>
                <i className="fa fa-warning text-danger" style={{ marginRight: 5 }} />
              </OverlayTrigger>)}
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

