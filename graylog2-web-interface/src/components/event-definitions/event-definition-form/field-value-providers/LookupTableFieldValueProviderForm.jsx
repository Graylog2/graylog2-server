import React from 'react';
import PropTypes from 'prop-types';
import { Col, ControlLabel, FormGroup, HelpBlock, Radio, Row } from 'react-bootstrap';
import lodash from 'lodash';
import naturalSort from 'javascript-natural-sort';

import { Select } from 'components/common';
import FormsUtils from 'util/FormsUtils';

class LookupTableFieldValueProviderForm extends React.Component {
  static propTypes = {
    allFieldTypes: PropTypes.array.isRequired,
    config: PropTypes.object.isRequired,
    eventFields: PropTypes.object.isRequired,
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
    const templateProvider = nextProviders.find(provider => provider.type === LookupTableFieldValueProviderForm.type);
    templateProvider[key] = value;
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

  formatFields = (context) => {
    const { allFieldTypes, eventFields } = this.props;

    if (context === 'source') {
      return this.formatMessageFields(allFieldTypes);
    }

    return Object.keys(eventFields)
      .sort(naturalSort)
      .map(fieldName => ({ label: fieldName, value: fieldName }));
  };

  formatLookupTables = (lookupTables) => {
    return lookupTables.map(table => ({ label: table.title, value: table.name }));
  };

  render() {
    const { config, lookupTables } = this.props;
    const provider = config.providers.find(p => p.type === LookupTableFieldValueProviderForm.type);

    return (
      <Row className="row-sm">
        <Col md={12}>
          <FormGroup>
            <ControlLabel>Take Lookup Table key from...</ControlLabel>
            <Radio id="lookup-message-key-context"
                   name="key_context"
                   value="source"
                   checked={provider.key_context === 'source'}
                   onChange={this.handleChange}>
              Source log message
            </Radio>
            <Radio id="lookup-event-key-context"
                   name="key_context"
                   value="event"
                   checked={provider.key_context === 'event'}
                   onChange={this.handleChange}>
              Generated Event
            </Radio>
          </FormGroup>

          <FormGroup controlId="lookup-provider-table">
            <ControlLabel>Lookup Table Key Field</ControlLabel>
            <Select name="lookup-provider-key"
                    placeholder="Select Lookup Table"
                    onChange={this.handleSelectChange('key_field')}
                    options={provider.key_context ? this.formatFields(provider.key_context) : []}
                    value={provider.key_field}
                    matchProp="label"
                    disabled={provider.key_context === undefined}
                    allowCreate
                    required />
            <HelpBlock>
              Field name whose value will be used as Lookup Table Key.
              {provider.key_context === 'event' && ' Please ensure the Event contains the given Field.'}
            </HelpBlock>
          </FormGroup>

          <FormGroup controlId="lookup-provider-table">
            <ControlLabel>Select Lookup Table</ControlLabel>
            <Select name="event-field-provider"
                    placeholder="Select Lookup Table"
                    onChange={this.handleSelectChange('table_name')}
                    options={this.formatLookupTables(lookupTables)}
                    value={provider.table_name}
                    matchProp="label"
                    required />
            <HelpBlock>Select the Lookup Table Graylog should use to get the value.</HelpBlock>
          </FormGroup>
        </Col>
      </Row>
    );
  }
}

export default LookupTableFieldValueProviderForm;
