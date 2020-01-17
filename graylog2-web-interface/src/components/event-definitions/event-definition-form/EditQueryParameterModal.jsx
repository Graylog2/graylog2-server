import PropTypes from 'prop-types';
import lodash from 'lodash';
import React from 'react';
import styled from 'styled-components';

import { Button, Panel, ControlLabel, FormGroup, HelpBlock } from 'components/graylog';
import { BootstrapModalForm, Input } from 'components/bootstrap';

import { Select } from 'components/common';
import FormsUtils from 'util/FormsUtils';
import { naturalSortIgnoreCase } from 'util/SortUtils';

const StyledPanel = styled(Panel)`
  margin-top: 20px;
`;

const StyledInlineCode = styled('code')`
  margin: 0 0.25em;
  white-space: nowrap;
`;

class EditQueryParameterModal extends React.Component {
  static propTypes = {
    eventDefinition: PropTypes.object.isRequired,
    queryParameter: PropTypes.object.isRequired,
    lookupTables: PropTypes.array.isRequired,
    onChange: PropTypes.func.isRequired,
  };

  constructor(props) {
    super(props);

    const { queryParameter } = this.props;
    this.state = {
      lastParameter: lodash.cloneDeep(queryParameter),
      validation: {},
    };
  }

  openModal = () => {
    this.modal.open();
  };

  _saved = () => {
    const { queryParameter } = this.props;
    if (this._hasErrors()) {
      return;
    }
    // TODO find a nicer way to handle this
    this.setState({ lastParameter: queryParameter });
    this.modal.close();
  };

  _cleanState = () => {
    const { eventDefinition, onChange, queryParameter } = this.props;
    const config = lodash.cloneDeep(eventDefinition.config);
    const queryParameters = config.query_parameters;
    const idx = queryParameters.findIndex(p => p.name === queryParameter.name);
    if (idx === -1) {
      throw new Error(`Query parameter "${queryParameter.name}" not found`);
    }
    const { lastParameter } = this.state;
    queryParameters[idx] = lastParameter;
    config.query_parameters = queryParameters;
    onChange('config', config);
  };

  propagateChanges = (key, value) => {
    const { eventDefinition, onChange, queryParameter } = this.props;
    const config = lodash.cloneDeep(eventDefinition.config);
    const queryParameters = config.query_parameters;
    const idx = queryParameters.findIndex(p => p.name === queryParameter.name);
    if (idx === -1) {
      throw new Error(`Query parameter "${queryParameter.name}" not found`);
    }

    queryParameters[idx][key] = value;

    if (this._validate(queryParameters[idx])) {
      delete queryParameters[idx].embryonic;
    } else {
      queryParameters[idx].embryonic = true;
    }
    config.query_parameters = queryParameters;
    onChange('config', config);
  };

  handleSelectChange = (key) => {
    return (nextLookupTable) => {
      this.propagateChanges(key, nextLookupTable);
    };
  };

  handleChange = (event) => {
    const { name } = event.target;
    const value = FormsUtils.getValueFromInput(event.target);
    this.propagateChanges(name, value);
  };

  _validate = (queryParameter) => {
    const newValidation = {};
    if (!queryParameter.lookup_table) {
      newValidation.lookup_table = 'Cannot be empty';
    }
    if (!queryParameter.key) {
      newValidation.key = 'Cannot be empty';
    }
    this.setState({ validation: newValidation });
    return lodash.isEmpty(newValidation);
  };

  _hasErrors = () => {
    const { validation } = this.state;
    return !lodash.isEmpty(validation);
  };

  formatLookupTables = (lookupTables) => {
    if (!lookupTables) {
      return [];
    }
    return lookupTables
      .sort((lt1, lt2) => naturalSortIgnoreCase(lt1.title, lt2.title))
      .map(table => ({ label: table.title, value: table.name }));
  };

  render() {
    const { queryParameter, lookupTables } = this.props;
    const { validation } = this.state;
    const parameterSyntax = `$${queryParameter.name}$`;
    return (
      <React.Fragment>
        <Button bsSize="small"
                bsStyle={queryParameter.embryonic ? 'primary' : 'info'}
                onClick={() => this.openModal()}>
          {queryParameter.name}
        </Button>

        <BootstrapModalForm ref={(ref) => { this.modal = ref; }}
                            title={`Declare Query Parameter "${queryParameter.name}" from Lookup Table`}
                            onSubmitForm={this._saved}
                            onModalClose={this._cleanState}
                            submitButtonDisabled={this._hasErrors()}
                            submitButtonText="Save">

          <FormGroup controlId="lookup-provider-table" validationState={validation.lookup_table ? 'error' : null}>
            <ControlLabel>Select Lookup Table</ControlLabel>
            <Select name="query-param-table-name"
                    placeholder="Select Lookup Table"
                    onChange={this.handleSelectChange('lookup_table')}
                    options={this.formatLookupTables(lookupTables)}
                    value={queryParameter.lookup_table}
                    autoFocus
                    required />
            <HelpBlock>
              {validation.lookup_table || 'Select the Lookup Table Graylog should use to get the values.'}
            </HelpBlock>
          </FormGroup>
          <Input type="text"
                 id={`key-${queryParameter.name}`}
                 label="Lookup Table Key"
                 name="key"
                 defaultValue={queryParameter.key}
                 onChange={this.handleChange}
                 bsStyle={validation.key ? 'error' : null}
                 help={validation.key ? validation.key : 'Select the Lookup Table Key'}
                 spellCheck={false}
                 required />
          <Input id={`default-value-${queryParameter.name}`}
                 type="text"
                 name="default_value"
                 label="Default Value"
                 help="Select a default value in case the lookup result is empty"
                 defaultValue={queryParameter.default_value}
                 spellCheck={false}
                 onChange={this.handleChange} />
          <StyledPanel header="How to use">
            <h5>General Usage</h5>
            <p>
              After declaring it, the parameter
              <StyledInlineCode>{parameterSyntax}</StyledInlineCode>
              in your query, will be replaced with the list of results from the lookup table.
              The list of results will be presented in the form of a Lucene BooleanQuery. E.g.:
              <StyledInlineCode>(&quot;foo&quot; OR &quot;bar&quot; OR &quot;baz&quot;)</StyledInlineCode>
            </p>
            <h5>Behaviour on empty lookup result list</h5>
            <p>
              The event definition query is only executed if a value for the parameter is present.
              If the lookup result is empty, the execution will be skipped and treated as if the <i>Search Query</i> found
              no messages. If an execution is desired a <i>Default Value</i> that yields the desired search result
              needs to be provided. For example, (depending on the use case) a wildcard like
              <StyledInlineCode>*</StyledInlineCode>
              can be a meaningful Default Value.
            </p>
            <h5>Limitations</h5>
            <p>
              Please note that maximum number of supported results is 1024. If the lookup table returns
              more results, the event definition is not executed.
            </p>
          </StyledPanel>
        </BootstrapModalForm>
      </React.Fragment>
    );
  }
}

export default EditQueryParameterModal;
