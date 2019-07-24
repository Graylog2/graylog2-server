import React from 'react';
import PropTypes from 'prop-types';
import { Col, ControlLabel, FormGroup, HelpBlock, Row } from 'react-bootstrap';
import lodash from 'lodash';
import naturalSort from 'javascript-natural-sort';

import { Select } from 'components/common';
import FormsUtils from 'util/FormsUtils';

class LookupTableFieldValueProviderForm extends React.Component {
  static propTypes = {
    allFieldTypes: PropTypes.array.isRequired,
    config: PropTypes.object.isRequired,
    lookupTables: PropTypes.array.isRequired,
    onChange: PropTypes.func.isRequired,
  };

  static type = 'lookup-v1';

  formatMessageFields = lodash.memoize(
    (fieldTypes) => {
      return fieldTypes
        .sort((ftA, ftB) => naturalSort(ftA.name, ftB.name))
        .map((fieldType) => {
          return {
            label: `${fieldType.name} – ${fieldType.value.type.type}`,
            value: fieldType.name,
          };
        });
    },
    fieldTypes => fieldTypes.map(ft => ft.name).join('-'),
  );

  propagateChanges = (key, value) => {
    const { config, onChange } = this.props;
    const nextProviders = lodash.cloneDeep(config.providers);
    const lookupProvider = nextProviders.find(provider => provider.type === LookupTableFieldValueProviderForm.type);
    lookupProvider[key] = value;
    onChange(Object.assign({}, config, { providers: nextProviders }));
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
    return lookupTables.map(table => ({ label: table.title, value: table.name }));
  };

  render() {
    const { allFieldTypes, config, lookupTables } = this.props;
    const provider = config.providers.find(p => p.type === LookupTableFieldValueProviderForm.type);

    return (
      <Row className="row-sm">
        <Col md={7} lg={6}>
          <FormGroup controlId="lookup-provider-table">
            <ControlLabel>Select Lookup Table</ControlLabel>
            <Select name="event-field-table-name"
                    placeholder="Select Lookup Table"
                    onChange={this.handleSelectChange('table_name')}
                    options={this.formatLookupTables(lookupTables)}
                    value={provider.table_name}
                    matchProp="label"
                    required />
            <HelpBlock>Select the Lookup Table Graylog should use to get the value.</HelpBlock>
          </FormGroup>

          <FormGroup controlId="lookup-provider-table">
            <ControlLabel>Lookup Table Key Field</ControlLabel>
            <Select name="lookup-provider-key"
                    placeholder="Select Field"
                    onChange={this.handleSelectChange('key_field')}
                    options={this.formatMessageFields(allFieldTypes)}
                    value={provider.key_field}
                    matchProp="label"
                    allowCreate
                    required />
            <HelpBlock>Message Field name whose value will be used as Lookup Table Key.</HelpBlock>
          </FormGroup>
        </Col>
      </Row>
    );
  }
}

export default LookupTableFieldValueProviderForm;
