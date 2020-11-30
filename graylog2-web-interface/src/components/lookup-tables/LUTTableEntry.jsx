/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import PropTypes from 'prop-types';
import React from 'react';

import { LinkContainer, Link } from 'components/graylog/router';
import CombinedProvider from 'injection/CombinedProvider';
import Routes from 'routing/Routes';
import { Button } from 'components/graylog';
import { ErrorPopover } from 'components/lookup-tables';
import { ContentPackMarker } from 'components/common';

const { LookupTablesActions } = CombinedProvider.get('LookupTables');

class LUTTableEntry extends React.Component {
  static propTypes = {
    table: PropTypes.object.isRequired,
    cache: PropTypes.object.isRequired,
    dataAdapter: PropTypes.object.isRequired,
    errors: PropTypes.object,
  };

  static defaultProps = {
    errors: {
      table: null,
      cache: null,
      dataAdapter: null,
    },
  };

  _onDelete = () => {
    // eslint-disable-next-line no-alert
    if (window.confirm(`Are you sure you want to delete lookup table "${this.props.table.title}"?`)) {
      LookupTablesActions.delete(this.props.table.id).then(() => LookupTablesActions.reloadPage());
    }
  };

  render() {
    return (
      <tbody>
        <tr>
          <td>
            {this.props.errors.table && (<ErrorPopover placement="right" errorText={this.props.errors.table} title="Lookup Table problem" />) }
            <Link to={Routes.SYSTEM.LOOKUPTABLES.show(this.props.table.name)}>{this.props.table.title}</Link>
            <ContentPackMarker contentPack={this.props.table.content_pack} marginLeft={5} />
          </td>
          <td>{this.props.table.description}</td>
          <td>{this.props.table.name}</td>
          <td>
            {this.props.errors.cache && (<ErrorPopover placement="bottom" errorText={this.props.errors.cache} title="Cache problem" />) }
            <Link to={Routes.SYSTEM.LOOKUPTABLES.CACHES.show(this.props.cache.name)}>{this.props.cache.title}</Link>
          </td>
          <td>
            {this.props.errors.dataAdapter && (<ErrorPopover placement="bottom" errorText={this.props.errors.dataAdapter} title="Data adapter problem" />) }
            <Link to={Routes.SYSTEM.LOOKUPTABLES.DATA_ADAPTERS.show(this.props.dataAdapter.name)}>{this.props.dataAdapter.title}</Link>
          </td>
          <td>
            <LinkContainer to={Routes.SYSTEM.LOOKUPTABLES.edit(this.props.table.name)}>
              <Button bsSize="xsmall" bsStyle="info">Edit</Button>
            </LinkContainer>
          &nbsp;
            <Button bsSize="xsmall" bsStyle="primary" onClick={this._onDelete}>Delete</Button>
          </td>
        </tr>
      </tbody>
    );
  }
}

export default LUTTableEntry;
