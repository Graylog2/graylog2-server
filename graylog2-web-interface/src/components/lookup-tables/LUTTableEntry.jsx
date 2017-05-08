import React from 'react';
import { LinkContainer } from 'react-router-bootstrap';
import { Button, OverlayTrigger, Popover } from 'react-bootstrap';

import CombinedProvider from 'injection/CombinedProvider';

import Routes from 'routing/Routes';

const { LookupTablesActions } = CombinedProvider.get('LookupTables');

const LUTTableEntry = React.createClass({

  propTypes: {
    table: React.PropTypes.object.isRequired,
    cache: React.PropTypes.object.isRequired,
    dataAdapter: React.PropTypes.object.isRequired,
    errors: React.PropTypes.object,
  },

  getDefaultProps() {
    return {
      errors: {
        table: null,
        cache: null,
        dataAdapter: null,
      },
    };
  },

  _onDelete() {
// eslint-disable-next-line no-alert
    if (window.confirm(`Are you sure you want to delete lookup table "${this.props.table.title}"?`)) {
      LookupTablesActions.delete(this.props.table.id).then(() => LookupTablesActions.reloadPage());
    }
  },
  render() {
    let tableErrorOverlay = null;
    if (this.props.errors.table) {
      tableErrorOverlay = (<Popover id="popover-table-error" title="Lookup Table problem" style={{ maxWidth: 400 }}>
        {this.props.errors.table}
      </Popover>);
    }

    let dataAdapterErrorOverlay = null;
    if (this.props.errors.dataAdapter) {
      dataAdapterErrorOverlay = (<Popover id="popover-adapter-error" title="Data adapter problem" style={{ maxWidth: 400 }}>
        {this.props.errors.dataAdapter}
      </Popover>);
    }
    let cacheErrorOverlay = null;
    if (this.props.errors.cache) {
      cacheErrorOverlay = (<Popover id="popover-adapter-error" title="Cache problem" style={{ maxWidth: 400 }}>
        {this.props.errors.cache}
      </Popover>);
    }

    return (<tbody>
      <tr>
        <td>
          {this.props.errors.table && (
            <OverlayTrigger trigger={['hover', 'focus']} placement="right" overlay={tableErrorOverlay}>
              <i className="fa fa-warning text-danger" style={{ marginRight: 5 }} />
            </OverlayTrigger>)}
          <LinkContainer to={Routes.SYSTEM.LOOKUPTABLES.show(this.props.table.name)}><a>{this.props.table.title}</a></LinkContainer>
        </td>
        <td>{this.props.table.description}</td>
        <td>{this.props.table.name}</td>
        <td>
          {this.props.errors.cache && (
            <OverlayTrigger trigger={['hover', 'focus']} placement="bottom" overlay={cacheErrorOverlay}>
              <i className="fa fa-warning text-danger" style={{ marginRight: 5 }} />
            </OverlayTrigger>)}
          <LinkContainer to={Routes.SYSTEM.LOOKUPTABLES.CACHES.show(this.props.cache.name)}><a>{this.props.cache.title}</a></LinkContainer>
        </td>
        <td>
          {this.props.errors.dataAdapter && (
            <OverlayTrigger trigger={['hover', 'focus']} placement="bottom" overlay={dataAdapterErrorOverlay}>
              <i className="fa fa-warning text-danger" style={{ marginRight: 5 }} />
            </OverlayTrigger>)}
          <LinkContainer to={Routes.SYSTEM.LOOKUPTABLES.DATA_ADAPTERS.show(this.props.dataAdapter.name)}><a>{this.props.dataAdapter.title}</a></LinkContainer>
        </td>
        <td>
          <LinkContainer to={Routes.SYSTEM.LOOKUPTABLES.edit(this.props.table.name)}>
            <Button bsSize="xsmall" bsStyle="info">Edit</Button>
          </LinkContainer>
          &nbsp;
          <Button bsSize="xsmall" bsStyle="primary" onClick={this._onDelete}>Delete</Button>
        </td>
      </tr>
    </tbody>);
  },

});

export default LUTTableEntry;

