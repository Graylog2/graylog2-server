import PropTypes from 'prop-types';
import React from 'react';
import { LinkContainer } from 'react-router-bootstrap';
import { Link } from 'react-router';

import { Button } from 'react-bootstrap';

import Routes from 'routing/Routes';
import CombinedProvider from 'injection/CombinedProvider';

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
// eslint-disable-next-line no-alert
    if (window.confirm(`Are you sure you want to delete data adapter "${this.props.adapter.title}"?`)) {
      LookupTableDataAdaptersActions.delete(this.props.adapter.id).then(() => LookupTableDataAdaptersActions.reloadPage());
    }
  };

  render() {
    return (
      <tbody>
        <tr>
          <td>
            {this.props.error && <ErrorPopover errorText={this.props.error} title="Lookup table problem" placement="right" />}
            <Link to={Routes.SYSTEM.LOOKUPTABLES.DATA_ADAPTERS.show(this.props.adapter.name)}>{this.props.adapter.title}</Link>
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
  }
}

export default DataAdapterTableEntry;

