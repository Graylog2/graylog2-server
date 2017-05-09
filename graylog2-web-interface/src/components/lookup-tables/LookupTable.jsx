import React from 'react';
import { Button, Row, Col } from 'react-bootstrap';
import { Input } from 'components/bootstrap';
import { LinkContainer } from 'react-router-bootstrap';
import Routes from 'routing/Routes';

import FormsUtils from 'util/FormsUtils';

import CombinedProvider from 'injection/CombinedProvider';

const { LookupTablesActions } = CombinedProvider.get('LookupTables');

const LookupTable = React.createClass({

  propTypes: {
    table: React.PropTypes.object.isRequired,
    cache: React.PropTypes.object.isRequired,
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
    LookupTablesActions.lookup(this.props.table.name, this.state.lookupKey);
  },

  render() {
    return (
      <Row className="content">
        <Col md={6}>
          <h3>{this.props.table.title}</h3>
          <span>{this.props.table.description}</span>
          <dl>
            <dt>Data adapter</dt>
            <dd>
              <LinkContainer to={Routes.SYSTEM.LOOKUPTABLES.DATA_ADAPTERS.show(this.props.dataAdapter.name)}><a>{this.props.dataAdapter.title}</a></LinkContainer>
            </dd>
            <dt>Cache</dt>
            <dd><LinkContainer to={Routes.SYSTEM.LOOKUPTABLES.CACHES.show(this.props.cache.name)}><a>{this.props.cache.title}</a></LinkContainer></dd>
          </dl>
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

export default LookupTable;
