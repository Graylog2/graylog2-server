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
import React from 'react';
import PropTypes from 'prop-types';
import lodash from 'lodash';

import { Col, ControlLabel, FormGroup, HelpBlock, Row } from 'components/graylog';
import { Select } from 'components/common';
import FormsUtils from 'util/FormsUtils';
import { naturalSortIgnoreCase } from 'util/SortUtils';

class LookupTableFieldValueProviderForm extends React.Component {
  static propTypes = {
    allFieldTypes: PropTypes.array.isRequired,
    config: PropTypes.object.isRequired,
    validation: PropTypes.object.isRequired,
    lookupTables: PropTypes.array.isRequired,
    onChange: PropTypes.func.isRequired,
  };

  static type = 'lookup-v1';

  static defaultConfig = {
    table_name: '',
    key_field: '',
  };

  static requiredFields = [
    'table_name',
    'key_field',
  ];

  formatMessageFields = lodash.memoize(
    (fieldTypes) => {
      return fieldTypes
        .sort((ftA, ftB) => naturalSortIgnoreCase(ftA.name, ftB.name))
        .map((fieldType) => {
          return {
            label: `${fieldType.name} – ${fieldType.value.type.type}`,
            value: fieldType.name,
          };
        });
    },
    (fieldTypes) => fieldTypes.map((ft) => ft.name).join('-'),
  );

  propagateChanges = (key, value) => {
    const { config, onChange } = this.props;
    const nextProviders = lodash.cloneDeep(config.providers);
    const lookupProvider = nextProviders.find((provider) => provider.type === LookupTableFieldValueProviderForm.type);

    lookupProvider[key] = value;
    onChange({ ...config, providers: nextProviders });
  };

  handleChange = (event) => {
    const { name } = event.target;
    const value = FormsUtils.getValueFromInput(event.target);

    this.propagateChanges(name, value);
  };

  handleSelectChange = (key) => {
    return (nextLookupTable) => {
      this.propagateChanges(key, nextLookupTable);
    };
  };

  formatLookupTables = (lookupTables) => {
    return lookupTables
      .sort((lt1, lt2) => naturalSortIgnoreCase(lt1.title, lt2.title))
      .map((table) => ({ label: table.title, value: table.name }));
  };

  render() {
    const { allFieldTypes, config, lookupTables, validation } = this.props;
    const provider = config.providers.find((p) => p.type === LookupTableFieldValueProviderForm.type);

    return (
      <Row className="row-sm">
        <Col md={7} lg={6}>
          <FormGroup controlId="lookup-provider-table" validationState={validation.errors.table_name ? 'error' : null}>
            <ControlLabel>Select Lookup Table</ControlLabel>
            <Select name="event-field-table-name"
                    placeholder="Select Lookup Table"
                    onChange={this.handleSelectChange('table_name')}
                    options={this.formatLookupTables(lookupTables)}
                    value={provider.table_name}
                    matchProp="label"
                    required />
            <HelpBlock>
              {validation.errors.table_name || 'Select the Lookup Table Graylog should use to get the value.'}
            </HelpBlock>
          </FormGroup>

          <FormGroup controlId="lookup-provider-table" validationState={validation.errors.key_field ? 'error' : null}>
            <ControlLabel>Lookup Table Key Field</ControlLabel>
            <Select name="lookup-provider-key"
                    placeholder="Select Field"
                    onChange={this.handleSelectChange('key_field')}
                    options={this.formatMessageFields(allFieldTypes)}
                    value={provider.key_field}
                    matchProp="label"
                    allowCreate
                    required />
            <HelpBlock>
              {validation.errors.key_field || 'Message Field name whose value will be used as Lookup Table Key.'}
            </HelpBlock>
          </FormGroup>
        </Col>
      </Row>
    );
  }
}

export default LookupTableFieldValueProviderForm;
