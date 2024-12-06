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
import * as React from 'react';
import styled from 'styled-components';

import { Panel, Input } from 'components/bootstrap';
import Select from 'components/common/Select';
import { naturalSortIgnoreCase } from 'util/SortUtils';
import Spinner from 'components/common/Spinner';
import type { LookupTable } from 'logic/lookup-tables/types';
import type { ValidationState } from 'components/common/types';

const StyledInlineCode = styled('code')`
  margin: 0 0.25em;
  white-space: nowrap;
`;

type Props = {
  onChange: (fieldName: string, value: string) => void
  lookupTables: Array<LookupTable>
  identifier: string | number,
  defaultExpandHelp?: boolean,
  parameter?: {
    lookupTable?: string,
    key?: string,
    defaultValue?: string
    name?: string,
  },
  validationState?: {
    lookupTable?: [ValidationState, string],
    key?: [ValidationState, string],
  }
};

const LookupTableParameterEdit = ({
  validationState = {},
  onChange,
  lookupTables,
  identifier,
  parameter = {},
  defaultExpandHelp = true,
}: Props) => {
  const { lookupTable, key: tableKey, defaultValue, name } = parameter;
  const parameterSyntax = `$${name}$`;

  const _handleChange = (fieldName: string) => (value) => {
    onChange(fieldName, value);
  };

  const _handleInputChange = (attributeName: string) => ({ target: { value } }: React.ChangeEvent<HTMLInputElement>) => _handleChange(attributeName)(value);

  if (!lookupTables) {
    return <Spinner text="Loading lookup tables" />;
  }

  const lookupTableOptions = lookupTables.sort((lt1, lt2) => naturalSortIgnoreCase(lt1.title, lt2.title))
    .map((table) => ({ label: table.title, value: table.name }));

  return (
    <>
      <Input id={`lookup-table-parameter-table-${identifier}`}
             name="query-param-table-name"
             label="Lookup Table"
             bsStyle={validationState?.lookupTable?.[0]}
             error={validationState?.lookupTable?.[1]}
             help="Select the lookup table Graylog should use to get the values.">
        <Select placeholder="Select lookup table"
                onChange={_handleChange('lookupTable')}
                options={lookupTableOptions}
                value={lookupTable}
                autoFocus
                clearable={false}
                required />
      </Input>
      <Input type="text"
             id={`lookup-table-parameter-key-${identifier}`}
             label="Lookup Table Key"
             name="key"
             defaultValue={tableKey}
             onChange={_handleInputChange('key')}
             bsStyle={validationState?.key?.[0]}
             help="Select the lookup table key"
             error={validationState?.key?.[0] === 'error' ? validationState?.key?.[1] : undefined}
             spellCheck={false}
             required />
      <Input id={`lookup-table-parameter-default-value-${identifier}`}
             type="text"
             name="defaultValue"
             label="Default Value"
             help="Select a default value in case the lookup result is empty"
             defaultValue={defaultValue}
             spellCheck={false}
             onChange={_handleInputChange('defaultValue')} />

      <Panel id="lookup-table-parameter-help" defaultExpanded={defaultExpandHelp}>
        <Panel.Heading>
          <Panel.Title toggle>
            How to use lookup table parameters
          </Panel.Title>
        </Panel.Heading>
        <Panel.Collapse>
          <Panel.Body>
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
              more results, the query is not executed.
            </p>
          </Panel.Body>
        </Panel.Collapse>
      </Panel>
    </>
  );
};

export default LookupTableParameterEdit;
