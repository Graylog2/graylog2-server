import React from 'react';
import PropTypes from 'prop-types';
import { Col, ControlLabel, FormGroup, HelpBlock, Row } from 'components/graylog';
import lodash from 'lodash';

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
