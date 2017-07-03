import React from 'react';
import { Button, ButtonToolbar, Row, Col } from 'react-bootstrap';
import { Input } from 'components/bootstrap';
import { LinkContainer } from 'react-router-bootstrap';
import Routes from 'routing/Routes';

import FormsUtils from 'util/FormsUtils';
import { ContentPackMarker } from 'components/common';

import CombinedProvider from 'injection/CombinedProvider';

const { LookupTablesActions } = CombinedProvider.get('LookupTables');

const LookupTable = React.createClass({

  propTypes: {
    table: React.PropTypes.object.isRequired,
    cache: React.PropTypes.object.isRequired,
    dataAdapter: React.PropTypes.object.isRequired,
  },

  getInitialState() {
    return {
      lookupKey: null,
      lookupResult: null,
      purgeKey: null,
    };
  },

  _onChange(event) {
    this.setState({ lookupKey: FormsUtils.getValueFromInput(event.target) });
  },

  _onChangePurgeKey(event) {
    this.setState({ purgeKey: FormsUtils.getValueFromInput(event.target) });
  },

  _onPurgeKey(e) {
    e.preventDefault();
    if (this.state.purgeKey && this.state.purgeKey.length > 0) {
      LookupTablesActions.purgeKey(this.props.table, this.state.purgeKey);
    }
  },

  _onPurgeAll(e) {
    e.preventDefault();
    LookupTablesActions.purgeAll(this.props.table);
  },

  _lookupKey(e) {
    e.preventDefault();
    LookupTablesActions.lookup(this.props.table.name, this.state.lookupKey).then((result) => {
      this.setState({ lookupResult: result });
    });
  },

  render() {
    return (
      <Row className="content">
        <Col md={6}>
          <h3>
            {this.props.table.title}
            <ContentPackMarker contentPack={this.props.table.content_pack} marginLeft={5} />
          </h3>
          <span>{this.props.table.description}</span>
          <dl>
            <dt>Data adapter</dt>
            <dd>
              <LinkContainer to={Routes.SYSTEM.LOOKUPTABLES.DATA_ADAPTERS.show(this.props.dataAdapter.name)}><a>{this.props.dataAdapter.title}</a></LinkContainer>
            </dd>
            <dt>Cache</dt>
            <dd><LinkContainer to={Routes.SYSTEM.LOOKUPTABLES.CACHES.show(this.props.cache.name)}><a>{this.props.cache.title}</a></LinkContainer></dd>
          </dl>
          {
            (this.props.table.default_single_value || this.props.table.default_multi_value) &&
            <dl>
              <dt>Default single value</dt>
              <dd><code>{this.props.table.default_single_value}</code>{' '}({this.props.table.default_single_value_type.toLowerCase()})</dd>
              <dt>Default multi value</dt>
              <dd><code>{this.props.table.default_multi_value}</code>{' '}({this.props.table.default_multi_value_type.toLowerCase()})</dd>
            </dl>
          }
          <h3>Purge Cache</h3>
          <p>You can purge the complete cache for this lookup table or only the cache entry for a single key.</p>
          <form onSubmit={this._onPurgeKey}>
            <fieldset>
              <Input type="text"
                     id="purge-key"
                     name="purge-key"
                     label="Key"
                     onChange={this._onChangePurgeKey}
                     help="Key to purge from cache"
                     required
                     value={this.state.purgeKey} />
              <Input>
                <ButtonToolbar>
                  <Button type="submit" bsStyle="success">Purge key</Button>
                  <Button type="button" bsStyle="info" onClick={this._onPurgeAll}>Purge all</Button>
                </ButtonToolbar>
              </Input>
            </fieldset>
          </form>
        </Col>
        <Col md={6}>
          <h3>Test lookup</h3>
          <p>You can manually query the lookup table using this form. The data will be cached as configured by Graylog.</p>
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
          { this.state.lookupResult && (
            <div>
              <h4>Lookup result</h4>
              <pre>{JSON.stringify(this.state.lookupResult, null, 2)}</pre>
            </div>
          )}
        </Col>
      </Row>
    );
  },

});

export default LookupTable;
