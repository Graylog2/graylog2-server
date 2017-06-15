import React from 'react';
import { LinkContainer } from 'react-router-bootstrap';

import { Button } from 'react-bootstrap';

import Routes from 'routing/Routes';
import CombinedProvider from 'injection/CombinedProvider';

import { ErrorPopover } from 'components/lookup-tables';
import { ContentPackMarker } from 'components/common';
import { MetricContainer, CounterRate } from 'components/metrics';

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
    return (
      <tbody>
        <tr>
          <td>
            {this.props.error && <ErrorPopover errorText={this.props.error} title="Lookup table problem" placement="right" />}
            <LinkContainer to={Routes.SYSTEM.LOOKUPTABLES.DATA_ADAPTERS.show(this.props.adapter.name)}><a>{this.props.adapter.title}</a></LinkContainer>
            <ContentPackMarker contentPack={this.props.adapter.content_pack} marginLeft={5} />
          </td>
          <td>{this.props.adapter.description}</td>
          <td>{this.props.adapter.name}</td>
          <td>
            <MetricContainer name={`org.graylog2.lookup.adapters.${this.props.adapter.id}.requests`}>
              <CounterRate suffix="lookups/s" />
            </MetricContainer>
          </td>
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

