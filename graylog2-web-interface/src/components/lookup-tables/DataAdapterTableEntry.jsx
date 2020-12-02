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
import Routes from 'routing/Routes';
import CombinedProvider from 'injection/CombinedProvider';
import { Button } from 'components/graylog';
import { ErrorPopover } from 'components/lookup-tables';
import { ContentPackMarker } from 'components/common';
import { MetricContainer, CounterRate } from 'components/metrics';

const { LookupTableDataAdaptersActions } = CombinedProvider.get('LookupTableDataAdapters');

class DataAdapterTableEntry extends React.Component {
  static propTypes = {
    adapter: PropTypes.object.isRequired,
    error: PropTypes.string,
  };

  static defaultProps = {
    error: null,
  };

  _onDelete = () => {
    const { adapter } = this.props;

    // eslint-disable-next-line no-alert
    if (window.confirm(`Are you sure you want to delete data adapter "${adapter.title}"?`)) {
      LookupTableDataAdaptersActions.delete(adapter.id).then(() => LookupTableDataAdaptersActions.reloadPage());
    }
  };

  render() {
    const { adapter, error } = this.props;

    return (
      <tbody>
        <tr>
          <td>
            {error && <ErrorPopover errorText={error} title="Lookup table problem" placement="right" />}
            <Link to={Routes.SYSTEM.LOOKUPTABLES.DATA_ADAPTERS.show(adapter.name)}>{adapter.title}</Link>
            <ContentPackMarker contentPack={adapter.content_pack} marginLeft={5} />
          </td>
          <td>{adapter.description}</td>
          <td>{adapter.name}</td>
          <td>
            <MetricContainer name={`org.graylog2.lookup.adapters.${adapter.id}.requests`}>
              <CounterRate suffix="lookups/s" />
            </MetricContainer>
          </td>
          <td>
            <LinkContainer to={Routes.SYSTEM.LOOKUPTABLES.DATA_ADAPTERS.edit(adapter.name)}>
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

export default DataAdapterTableEntry;
