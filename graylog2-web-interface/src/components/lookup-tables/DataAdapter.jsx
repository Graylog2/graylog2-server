import React from 'react';
import { Button, Row, Col } from 'react-bootstrap';
import { Input } from 'components/bootstrap';
import FormsUtils from 'util/FormsUtils';
import { PluginStore } from 'graylog-web-plugin/plugin';
import CombinedProvider from 'injection/CombinedProvider';
import Styles from './ConfigSummary.css';

const { LookupTableDataAdaptersActions } = CombinedProvider.get('LookupTableDataAdapters');

const DataAdapter = React.createClass({

  propTypes: {
    dataAdapter: React.PropTypes.object.isRequired,
    lookupResult: React.PropTypes.object,
  },

  getDefaultProps() {
    return {
      lookupResult: null,
    };
  },

  getInitialState() {
    return {
      lookupKey: null,
    };
  },

  _onChange(event) {
    this.setState({ lookupKey: FormsUtils.getValueFromInput(event.target) });
  },

  _lookupKey(e) {
    e.preventDefault();
    LookupTableDataAdaptersActions.lookup(this.props.dataAdapter.name, this.state.lookupKey);
  },

  render() {
    const plugins = {};
    PluginStore.exports('lookupTableAdapters').forEach((p) => {
      plugins[p.type] = p;
    });

    const dataAdapter = this.props.dataAdapter;
    const plugin = plugins[dataAdapter.config.type];
    if (!plugin) {
      return <p>Unknown data adapter type {dataAdapter.config.type}. Is the plugin missing?</p>;
    }

    const summary = plugin.summaryComponent;
    return (
      <Row className="content">
        <Col md={6}>
          <h2>{dataAdapter.title} <small>({plugin.displayName})</small></h2>
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
                     value={this.state.lookupKey} />
            </fieldset>
            <fieldset>
              <Input>
                <Button type="submit" bsStyle="success">Look up</Button>
              </Input>
            </fieldset>
          </form>
          { this.props.lookupResult && (
            <div>
              <h4>Lookup result</h4>
              <pre>{JSON.stringify(this.props.lookupResult, null, 2)}</pre>
            </div>
          )}
        </Col>
      </Row>
    );
  },

});

export default DataAdapter;
