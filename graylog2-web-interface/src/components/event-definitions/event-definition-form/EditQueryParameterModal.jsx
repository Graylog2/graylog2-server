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
import lodash from 'lodash';
import React from 'react';
import styled from 'styled-components';

import { Button, Panel, ControlLabel, FormGroup, HelpBlock } from 'components/graylog';
import { BootstrapModalForm, Input } from 'components/bootstrap';
import { Select } from 'components/common';
import * as FormsUtils from 'util/FormsUtils';
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
      queryParameter: lodash.cloneDeep(queryParameter),
      validation: {},
    };
  }

  openModal = () => {
    this.modal.open();
  };

  _saved = () => {
    const { queryParameter } = this.state;

    if (!this._validate(queryParameter)) {
      return;
    }

    this.propagateChanges();
    this.modal.close();
  };

  _cleanState = () => {
    const { queryParameter } = this.props;

    this.setState({ queryParameter: lodash.cloneDeep(queryParameter) });
  }

  propagateChanges = () => {
    const { eventDefinition, onChange, queryParameter: prevQueryParameter } = this.props;
    const { queryParameter } = this.state;
    const config = lodash.cloneDeep(eventDefinition.config);
    const { query_parameters: queryParameters } = config;
    const index = queryParameters.findIndex((p) => p.name === prevQueryParameter.name);

    if (index < 0) {
      throw new Error(`Query parameter "${queryParameter.name}" not found`);
    }

    queryParameters[index] = lodash.omit(queryParameter, 'embryonic');
    onChange('config', config);
  };

  handleParameterChange = (key, value) => {
    const { queryParameter } = this.state;
    const nextQueryParameter = { ...queryParameter, [key]: value };

    this.setState({ queryParameter: nextQueryParameter });
  };

  handleSelectChange = (key) => {
    return (nextLookupTable) => {
      this.handleParameterChange(key, nextLookupTable);
    };
  };

  handleChange = (event) => {
    const { name } = event.target;
    const value = FormsUtils.getValueFromInput(event.target);

    this.handleParameterChange(name, value);
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

  formatLookupTables = (lookupTables) => {
    if (!lookupTables) {
      return [];
    }

    return lookupTables
      .sort((lt1, lt2) => naturalSortIgnoreCase(lt1.title, lt2.title))
      .map((table) => ({ label: table.title, value: table.name }));
  };

  render() {
    const { lookupTables } = this.props;
    const { queryParameter, validation } = this.state;
    const parameterSyntax = `$${queryParameter.name}$`;

    return (
      <>
        <Button bsSize="small"
                bsStyle={queryParameter.embryonic ? 'primary' : 'info'}
                onClick={() => this.openModal()}>
          {queryParameter.name}{queryParameter.embryonic && ': undeclared'}
        </Button>

        <BootstrapModalForm ref={(ref) => { this.modal = ref; }}
                            title={`Declare Query Parameter "${queryParameter.name}" from Lookup Table`}
                            onSubmitForm={this._saved}
                            onModalClose={this._cleanState}
                            submitButtonText="Save">

          <FormGroup controlId="lookup-provider-table" validationState={validation.lookup_table ? 'error' : null}>
            <ControlLabel>Select Lookup Table</ControlLabel>
            <Select name="query-param-table-name"
                    placeholder="Select Lookup Table"
                    onChange={this.handleSelectChange('lookup_table')}
                    options={this.formatLookupTables(lookupTables)}
                    value={queryParameter.lookup_table}
                    autoFocus
                    clearable={false}
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
      </>
    );
  }
}

export default EditQueryParameterModal;
