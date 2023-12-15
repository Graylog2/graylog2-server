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
import memoize from 'lodash/memoize';

import useFieldTypes from 'views/logic/fieldtypes/useFieldTypes';
import { Col, ControlLabel, FormGroup, HelpBlock, Row } from 'components/bootstrap';
import { Select, Spinner } from 'components/common';
import { naturalSortIgnoreCase } from 'util/SortUtils';
import { useStore } from 'stores/connect';
import { isPermitted } from 'util/PermissionsMixin';
import useCurrentUser from 'hooks/useCurrentUser';
import { ALL_MESSAGES_TIMERANGE } from 'views/Constants';
import { LookupTablesActions, LookupTablesStore } from 'stores/lookup-tables/LookupTablesStore';

const LOOKUP_PERMISSIONS = [
  'lookuptables:read',
];

type Props = {
  onTableNameChange: () => void,
  onKeyChange: () => void,
  selectedTableName: string,
  selectedKeyName: string,
  nameValidation: string,
  keyValidation: string,
  lookupTableNameLabel?: string,
  lookupTableKeyLabel?: string,
  customKeyField?: React.ReactNode,
};

const LookupTableFields = ({ onTableNameChange, onKeyChange, selectedTableName, selectedKeyName, nameValidation, keyValidation, lookupTableNameLabel = '', lookupTableKeyLabel = '', customKeyField }: Props) => {
  const { data: allFieldTypes } = useFieldTypes([], ALL_MESSAGES_TIMERANGE);
  const lookupTables = useStore(LookupTablesStore);
  const currentUser = useCurrentUser();

  React.useEffect(() => {
    if (!isPermitted(currentUser.permissions, LOOKUP_PERMISSIONS)) {
      return;
    }

    LookupTablesActions.searchPaginated(1, 0, undefined, false);
  }, [currentUser.permissions]);

  if (!isPermitted(currentUser.permissions, LOOKUP_PERMISSIONS)) {
    return (
      <Row>
        <Col md={6} lg={5}>
          <p>No Lookup Tables found.</p>
        </Col>
      </Row>
    );
  }

  const formatMessageFields = memoize(
    (fieldTypes) => fieldTypes
      .sort((ftA, ftB) => naturalSortIgnoreCase(ftA.name, ftB.name))
      .map((fieldType) => ({
        label: `${fieldType.name} – ${fieldType.value.type.type}`,
        value: fieldType.name,
      })),
    (fieldTypes) => fieldTypes.map((ft) => ft.name).join('-'),
  );

  const formatLookupTables = (tables) => tables
    .sort((lt1, lt2) => naturalSortIgnoreCase(lt1.title, lt2.title))
    .map((table) => ({ label: table.title, value: table.name }));

  const isLoading = !allFieldTypes || !lookupTables.tables;

  if (isLoading) {
    return <Spinner text="Loading Field Provider information..." />;
  }

  return (
    <Row className="row-sm">
      <Col md={6}>
        <FormGroup controlId="lookup-provider-table" validationState={nameValidation ? 'error' : null}>
          <ControlLabel>{lookupTableNameLabel || 'Select Lookup Table'}</ControlLabel>
          <Select name="event-field-table-name"
                  placeholder="Select Lookup Table"
                  onChange={onTableNameChange}
                  options={formatLookupTables(lookupTables.tables)}
                  value={selectedTableName}
                  matchProp="label"
                  required />
          <HelpBlock>
            {nameValidation || 'Select the Lookup Table Graylog should use to get the value.'}
          </HelpBlock>
        </FormGroup>
      </Col>
      {customKeyField ? (
        <Col md={6}>
          {customKeyField}
        </Col>
      ) : (
        <Col md={6}>
          <FormGroup controlId="lookup-provider-table" validationState={keyValidation ? 'error' : null}>
            <ControlLabel>{lookupTableKeyLabel || 'Lookup Table Key Field'}</ControlLabel>
            <Select name="lookup-provider-key"
                    placeholder="Select Field"
                    onChange={onKeyChange}
                    options={formatMessageFields(allFieldTypes)}
                    value={selectedKeyName}
                    matchProp="label"
                    allowCreate
                    required />
            <HelpBlock>
              {keyValidation || 'Message Field name whose value will be used as Lookup Table Key.'}
            </HelpBlock>
          </FormGroup>
        </Col>
      )}
    </Row>
  );
};

LookupTableFields.defaultProps = {
  lookupTableNameLabel: undefined,
  lookupTableKeyLabel: undefined,
  customKeyField: null,
};

export default LookupTableFields;
