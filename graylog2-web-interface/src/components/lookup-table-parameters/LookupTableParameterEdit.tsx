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
import { Trans, useTranslation } from 'react-i18next';
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
  onChange: (fieldName: string, value: string) => void;
  lookupTables: Array<LookupTable>;
  identifier: string | number;
  defaultExpandHelp?: boolean;
  parameter?: {
    lookupTable?: string;
    key?: string;
    defaultValue?: string;
    name?: string;
  };
  validationState?: {
    lookupTable?: [ValidationState, string];
    key?: [ValidationState, string];
  };
};

const EXAMPLE_QUERY = '("foo" OR "bar" OR "baz")';
const WILDCARD = '*';
const MAX_CLAUSE_COUNT_SETTING = 'opensearch_indices_query_bool_max_clause_count';

const LookupTableParameterEdit = ({
  validationState = {},
  onChange,
  lookupTables,
  identifier,
  parameter = {},
  defaultExpandHelp = true,
}: Props) => {
  const { t } = useTranslation('lookup_tables');
  const { lookupTable, key: tableKey, defaultValue, name } = parameter;
  const parameterSyntax = `$${name}$`;

  const _handleChange = (fieldName: string) => (value) => {
    onChange(fieldName, value);
  };

  const _handleInputChange =
    (attributeName: string) =>
    ({ target: { value } }: React.ChangeEvent<HTMLInputElement>) =>
      _handleChange(attributeName)(value);

  if (!lookupTables) {
    return <Spinner text={t('parameter_edit.loading')} />;
  }

  const lookupTableOptions = lookupTables
    .sort((lt1, lt2) => naturalSortIgnoreCase(lt1.title, lt2.title))
    .map((table) => ({ label: table.title, value: table.name }));

  return (
    <>
      <Input
        id={`lookup-table-parameter-table-${identifier}`}
        name="query-param-table-name"
        label={t('parameter_edit.table_label')}
        bsStyle={validationState?.lookupTable?.[0]}
        error={validationState?.lookupTable?.[1]}
        help={t('parameter_edit.table_help')}>
        <Select
          placeholder={t('parameter_edit.table_placeholder')}
          onChange={_handleChange('lookupTable')}
          options={lookupTableOptions}
          value={lookupTable}
          autoFocus
          clearable={false}
          required
        />
      </Input>
      <Input
        type="text"
        id={`lookup-table-parameter-key-${identifier}`}
        label={t('parameter_edit.key_label')}
        name="key"
        defaultValue={tableKey}
        onChange={_handleInputChange('key')}
        bsStyle={validationState?.key?.[0]}
        help={t('parameter_edit.key_help')}
        error={validationState?.key?.[0] === 'error' ? validationState?.key?.[1] : undefined}
        spellCheck={false}
        required
      />
      <Input
        id={`lookup-table-parameter-default-value-${identifier}`}
        type="text"
        name="defaultValue"
        label={t('parameter_edit.default_value_label')}
        help={t('parameter_edit.default_value_help')}
        defaultValue={defaultValue}
        spellCheck={false}
        onChange={_handleInputChange('defaultValue')}
      />

      <Panel id="lookup-table-parameter-help" defaultExpanded={defaultExpandHelp}>
        <Panel.Heading>
          <Panel.Title toggle>{t('parameter_edit.help_title')}</Panel.Title>
        </Panel.Heading>
        <Panel.Collapse>
          <Panel.Body>
            <h5>{t('parameter_edit.general_usage_title')}</h5>
            <p>
              <Trans
                t={t}
                i18nKey="parameter_edit.general_usage"
                values={{ parameter: parameterSyntax, example: EXAMPLE_QUERY }}
                components={{ inlineCode: <StyledInlineCode /> }}
              />
            </p>
            <h5>{t('parameter_edit.empty_result_title')}</h5>
            <p>
              <Trans
                t={t}
                i18nKey="parameter_edit.empty_result"
                values={{ wildcard: WILDCARD }}
                components={{ emphasis: <i />, inlineCode: <StyledInlineCode /> }}
              />
            </p>
            <h5>{t('parameter_edit.limitations_title')}</h5>
            <p>
              <Trans
                t={t}
                i18nKey="parameter_edit.limitations"
                values={{ setting: MAX_CLAUSE_COUNT_SETTING }}
                components={{ inlineCode: <StyledInlineCode /> }}
              />
            </p>
          </Panel.Body>
        </Panel.Collapse>
      </Panel>
    </>
  );
};

export default LookupTableParameterEdit;
