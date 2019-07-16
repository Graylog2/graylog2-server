import React from 'react';
import PropTypes from 'prop-types';
import { Col, ControlLabel, FormGroup, HelpBlock, Radio, Row } from 'react-bootstrap';
import lodash from 'lodash';

import { Input } from 'components/bootstrap';
import { Select } from 'components/common';
import FormsUtils from 'util/FormsUtils';

class LookupTableFieldValueProviderForm extends React.Component {
  static propTypes = {
    config: PropTypes.object.isRequired,
    lookupTables: PropTypes.array.isRequired,
    onChange: PropTypes.func.isRequired,
  };

  static type = 'lookup-v1';

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

  handleLookupTableChange = (nextLookupTable) => {
    this.propagateChanges('table_name', nextLookupTable);
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

          <Input id="lookup-provider-key"
                 name="key_field"
                 type="text"
                 label="Lookup Table Key Field"
                 onChange={this.handleChange}
                 value={provider.key_field || ''}
                 help="Field name whose value will be used as Lookup Table Key." />

          <FormGroup controlId="lookup-provider-table">
            <ControlLabel>Select Lookup Table</ControlLabel>
            <Select name="event-field-provider"
                    placeholder="Select Lookup Table"
                    onChange={this.handleLookupTableChange}
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
