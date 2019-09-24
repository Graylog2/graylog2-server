import React from 'react';
import PropTypes from 'prop-types';
import { LinkContainer } from 'react-router-bootstrap';

import { Row, Col, Button } from 'components/graylog';
import { Input } from 'components/bootstrap';
import { ContentPackMarker } from 'components/common';
import FormsUtils from 'util/FormsUtils';
import Routes from 'routing/Routes';
import { PluginStore } from 'graylog-web-plugin/plugin';
import CombinedProvider from 'injection/CombinedProvider';

import Styles from './ConfigSummary.css';

const { LookupTableDataAdaptersActions } = CombinedProvider.get('LookupTableDataAdapters');

class DataAdapter extends React.Component {
  static propTypes = {
    dataAdapter: PropTypes.object.isRequired,
  };

  state = {
    lookupKey: null,
    lookupResult: null,
  };

  _onChange = (event) => {
    this.setState({ lookupKey: FormsUtils.getValueFromInput(event.target) });
  };

  _lookupKey = (e) => {
    const { dataAdapter } = this.props;
    const { lookupKey } = this.state;

    e.preventDefault();
    LookupTableDataAdaptersActions.lookup(dataAdapter.name, lookupKey).then((result) => {
      this.setState({ lookupResult: result });
    });
  };

  render() {
    const plugins = {};
    PluginStore.exports('lookupTableAdapters').forEach((p) => {
      plugins[p.type] = p;
    });

    const { dataAdapter } = this.props;
    const { lookupKey, lookupResult } = this.state;
    const plugin = plugins[dataAdapter.config.type];
    if (!plugin) {
      return <p>Unknown data adapter type {dataAdapter.config.type}. Is the plugin missing?</p>;
    }

    const summary = plugin.summaryComponent;
    return (
      <Row className="content">
        <Col md={6}>
          <h2>
            {dataAdapter.title}
            <ContentPackMarker contentPack={dataAdapter.content_pack} marginLeft={5} />
            {' '}
            <small>({plugin.displayName})</small>
          </h2>
          <div className={Styles.config}>
            <dl>
              <dt>Description</dt>
              <dd>{dataAdapter.description || <em>No description.</em>}</dd>
            </dl>
          </div>
          <h4>Configuration</h4>
          <div className={Styles.config}>
            {React.createElement(summary, { dataAdapter: dataAdapter })}
          </div>
          <LinkContainer to={Routes.SYSTEM.LOOKUPTABLES.DATA_ADAPTERS.edit(dataAdapter.name)}>
            <Button bsStyle="success">Edit</Button>
          </LinkContainer>
        </Col>
        <Col md={6}>
          <h3>Test lookup</h3>
          <p>You can manually trigger the data adapter using this form. The data will be not cached.</p>
          <form onSubmit={this._lookupKey}>
            <fieldset>
              <Input type="text"
                     id="key"
                     name="key"
                     label="Key"
                     required
                     onChange={this._onChange}
                     help="Key to look up a value for."
                     value={lookupKey} />
              <Button type="submit" bsStyle="success">Look up</Button>
            </fieldset>
          </form>
          { lookupResult && (
            <div>
              <h4>Lookup result</h4>
              <pre>{JSON.stringify(lookupResult, null, 2)}</pre>
            </div>
          )}
        </Col>
      </Row>
    );
  }
}

export default DataAdapter;
