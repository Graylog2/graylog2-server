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
import { ButtonToolbar, Row, Col, Button } from 'components/graylog';
import { Input } from 'components/bootstrap';
import Routes from 'routing/Routes';
import FormsUtils from 'util/FormsUtils';
import { ContentPackMarker } from 'components/common';
import CombinedProvider from 'injection/CombinedProvider';

const { LookupTablesActions } = CombinedProvider.get('LookupTables');

class LookupTable extends React.Component {
  static propTypes = {
    table: PropTypes.object.isRequired,
    cache: PropTypes.object.isRequired,
    dataAdapter: PropTypes.object.isRequired,
  };

  state = {
    lookupKey: null,
    lookupResult: null,
    purgeKey: null,
  };

  _onChange = (event) => {
    this.setState({ lookupKey: FormsUtils.getValueFromInput(event.target) });
  };

  _onChangePurgeKey = (event) => {
    this.setState({ purgeKey: FormsUtils.getValueFromInput(event.target) });
  };

  _onPurgeKey = (e) => {
    e.preventDefault();

    if (this.state.purgeKey && this.state.purgeKey.length > 0) {
      LookupTablesActions.purgeKey(this.props.table, this.state.purgeKey);
    }
  };

  _onPurgeAll = (e) => {
    e.preventDefault();
    LookupTablesActions.purgeAll(this.props.table);
  };

  _lookupKey = (e) => {
    e.preventDefault();

    LookupTablesActions.lookup(this.props.table.name, this.state.lookupKey).then((result) => {
      this.setState({ lookupResult: result });
    });
  };

  render() {
    return (
      <Row className="content">
        <Col md={6}>
          <h2>
            {this.props.table.title}
            <ContentPackMarker contentPack={this.props.table.content_pack} marginLeft={5} />
          </h2>
          <p>{this.props.table.description}</p>
          <dl>
            <dt>Data adapter</dt>
            <dd>
              <Link to={Routes.SYSTEM.LOOKUPTABLES.DATA_ADAPTERS.show(this.props.dataAdapter.name)}>{this.props.dataAdapter.title}</Link>
            </dd>
            <dt>Cache</dt>
            <dd><Link to={Routes.SYSTEM.LOOKUPTABLES.CACHES.show(this.props.cache.name)}>{this.props.cache.title}</Link></dd>
          </dl>
          <LinkContainer to={Routes.SYSTEM.LOOKUPTABLES.edit(this.props.table.name)}>
            <Button bsStyle="success">Edit</Button>
          </LinkContainer>
          {
            (this.props.table.default_single_value || this.props.table.default_multi_value)
            && (
            <dl>
              <dt>Default single value</dt>
              <dd><code>{this.props.table.default_single_value}</code>{' '}({this.props.table.default_single_value_type.toLowerCase()})</dd>
              <dt>Default multi value</dt>
              <dd><code>{this.props.table.default_multi_value}</code>{' '}({this.props.table.default_multi_value_type.toLowerCase()})</dd>
            </dl>
            )
          }
          <hr />
          <h2>Purge Cache</h2>
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
              <ButtonToolbar>
                <Button type="submit" bsStyle="success">Purge key</Button>
                <Button type="button" bsStyle="info" onClick={this._onPurgeAll}>Purge all</Button>
              </ButtonToolbar>
            </fieldset>
          </form>
        </Col>
        <Col md={6}>
          <h2>Test lookup</h2>
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
              <Button type="submit" bsStyle="success">Look up</Button>
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
  }
}

export default LookupTable;
