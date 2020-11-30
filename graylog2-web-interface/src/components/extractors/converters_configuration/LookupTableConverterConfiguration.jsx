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

import { Link } from 'components/graylog/router';
import { Row, Col } from 'components/graylog';
import { Input } from 'components/bootstrap';
import { Select, Spinner } from 'components/common';
import Routes from 'routing/Routes';
import FormUtils from 'util/FormsUtils';
import CombinedProvider from 'injection/CombinedProvider';

const { LookupTablesActions } = CombinedProvider.get('LookupTables');

class LookupTableConverterConfiguration extends React.Component {
  static propTypes = {
    type: PropTypes.string.isRequired,
    configuration: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
  };

  state = {
    lookupTables: undefined,
  };

  componentDidMount() {
    this.props.onChange(this.props.type, this._getConverterObject());

    // TODO the 10k items is bad. we need a searchable/scrollable long list select box
    LookupTablesActions.searchPaginated(1, 10000, null).then((result) => {
      this.setState({ lookupTables: result.lookup_tables });
    });
  }

  _getConverterObject = (configuration) => {
    return { type: this.props.type, config: configuration || this.props.configuration };
  };

  _toggleConverter = (event) => {
    let converter;

    if (FormUtils.getValueFromInput(event.target) === true) {
      converter = this._getConverterObject();
    }

    this.props.onChange(this.props.type, converter);
  };

  _updateConfigValue = (key, value) => {
    const newConfig = this.props.configuration;

    newConfig[key] = value;
    this.props.onChange(this.props.type, this._getConverterObject(newConfig));
  };

  _onChange = (key) => {
    return (event) => this._updateConfigValue(key, FormUtils.getValueFromInput(event.target));
  };

  _onSelect = (key) => {
    return (value) => this._updateConfigValue(key, value);
  };

  render() {
    if (!this.state.lookupTables) {
      return <Spinner />;
    }

    const lookupTables = this.state.lookupTables.map((table) => {
      return { label: table.title, value: table.name };
    });

    const helpMessage = (
      <span>
        Lookup tables can be created <Link to={Routes.SYSTEM.LOOKUPTABLES.OVERVIEW}>here</Link>.
      </span>
    );

    return (
      <div className="xtrc-converter">
        <Input type="checkbox"
               ref={(converterEnabled) => { this.converterEnabled = converterEnabled; }}
               id={`enable-${this.props.type}-converter`}
               label="Convert value by using lookup table"
               wrapperClassName="col-md-offset-2 col-md-10"
               defaultChecked
               onChange={this._toggleConverter} />

        <Row className="row-sm">
          <Col md={9} mdOffset={2}>
            <div className="xtrc-converter-subfields">
              <Input id="lookup_table_name"
                     label="Lookup Table"
                     labelClassName="col-md-3"
                     wrapperClassName="col-md-9"
                     required={this.converterEnabled && this.converterEnabled.getChecked()}
                     help={helpMessage}>
                <Select placeholder="Select a lookup table"
                        clearable={false}
                        options={lookupTables}
                        matchProp="label"
                        onChange={this._onSelect('lookup_table_name')}
                        value={this.props.configuration.lookup_table_name} />
              </Input>
            </div>
          </Col>
        </Row>
      </div>
    );
  }
}

export default LookupTableConverterConfiguration;
