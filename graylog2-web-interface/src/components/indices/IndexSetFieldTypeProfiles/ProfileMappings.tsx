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

import { styled } from 'styled-components';
import React, { useState } from 'react';
import { FieldArray, Field, useFormikContext } from 'formik';

import type { IndexSetFieldTypeProfileForm } from 'components/indices/IndexSetFieldTypeProfiles/types';
import { IconButton, Select, Spinner, Pagination } from 'components/common';
import { Alert, Button, HelpBlock, Input } from 'components/bootstrap';
import StringUtils from 'util/StringUtils';
import {
  MAPPINGS_PAGE_SIZE,
  getLastPage,
  getPageOfRow,
  getRowIndexesWithErrors,
  getRowsMatchingSearch,
} from 'components/indices/IndexSetFieldTypeProfiles/helpers/mappingsPaging';

const SelectContainer = styled.div`
  flex-basis: 100%;
`;

const SelectGroup = styled.div`
  flex-grow: 1;
  display: flex;
  gap: 5px;
`;
const List = styled.div`
  display: flex;
  flex-direction: column;
`;
const StyledLabel = styled.h5`
  font-weight: bold;
  margin-bottom: 5px;
`;

const Item = styled.div`
  display: flex;
  gap: 5px;
`;

const MappingsToolbar = styled.div`
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 10px;
`;

const SearchContainer = styled.div`
  flex-grow: 1;
`;

type ProfileFormSelectProps = {
  onChange: (param: { target: { value: string; name: string } }) => void;
  options: Array<{ value: string; label: string; disabled?: boolean }>;
  error: string;
  name: string;
  value: string | undefined | null;
  placeholder: string;
  allowCreate: boolean;
};

const ProfileFormSelect = ({
  onChange,
  options,
  error,
  name,
  value,
  placeholder,
  allowCreate,
}: ProfileFormSelectProps) => (
  <SelectContainer>
    <Input error={error} name={name} id={name}>
      <Select
        options={options}
        value={value}
        inputId={name}
        onChange={(newVal) => {
          onChange({ target: { value: newVal, name } });
        }}
        aria-label={`Select ${name}`}
        placeholder={placeholder}
        allowCreate={allowCreate}
      />
    </Input>
  </SelectContainer>
);

type Props = {
  fields: Array<{ value: string; label: string; disabled?: boolean }>;
  fieldTypeOptions: Array<{ value: string; label: string }>;
  isLoading: boolean;
};

const ProfileMappings = ({ fields, fieldTypeOptions, isLoading }: Props) => {
  const [mappingsPage, setMappingsPage] = useState(1);
  const [mappingsSearch, setMappingsSearch] = useState('');
  const {
    errors,
    values: { customFieldMappings },
  } = useFormikContext<IndexSetFieldTypeProfileForm>();

  const isPaginated = customFieldMappings.length > MAPPINGS_PAGE_SIZE;
  const matchingRows = getRowsMatchingSearch(customFieldMappings, isPaginated ? mappingsSearch : '');
  const totalPages = getLastPage(matchingRows.length);
  const currentPage = Math.min(mappingsPage, totalPages);
  const firstRowPosition = (currentPage - 1) * MAPPINGS_PAGE_SIZE;
  const pageRows = matchingRows.slice(firstRowPosition, firstRowPosition + MAPPINGS_PAGE_SIZE);
  const rowsWithErrors = getRowIndexesWithErrors(errors.customFieldMappings);

  return (
    <FieldArray
      name="customFieldMappings"
      render={({ remove, unshift }) => (
        <>
          <StyledLabel>Set up mappings</StyledLabel>
          <HelpBlock>Here you can set up type mapping to any field.</HelpBlock>
          <MappingsToolbar>
            {isPaginated && (
              <SearchContainer>
                <Input
                  id="index-set-field-type-profile-mappings-search"
                  type="text"
                  placeholder="Search mappings by field name"
                  aria-label="Search mappings"
                  value={mappingsSearch}
                  onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
                    setMappingsSearch(e.target.value);
                    setMappingsPage(1);
                  }}
                  onKeyDown={(e: React.KeyboardEvent) => {
                    if (e.key === 'Enter') e.preventDefault();
                  }}
                />
              </SearchContainer>
            )}
            <Button
              bsStyle="success"
              onClick={() => {
                unshift({});
                setMappingsSearch('');
                setMappingsPage(1);
              }}
              name="plus"
              title="Add mapping">
              Add mapping
            </Button>
          </MappingsToolbar>
          {isPaginated && (
            <HelpBlock>
              {pageRows.length === 0
                ? 'No mappings match your search'
                : `Showing ${firstRowPosition + 1}-${firstRowPosition + pageRows.length} of ${matchingRows.length} mappings`}
            </HelpBlock>
          )}
          {isPaginated && rowsWithErrors.length > 0 && (
            <Alert bsStyle="danger">
              {rowsWithErrors.length} {StringUtils.pluralize(rowsWithErrors.length, 'mapping has', 'mappings have')}{' '}
              errors.{' '}
              <Button
                bsSize="xs"
                onClick={() => {
                  setMappingsSearch('');
                  setMappingsPage(getPageOfRow(rowsWithErrors[0]));
                }}>
                Show first error
              </Button>
            </Alert>
          )}
          <List>
            {isLoading ? (
              <Spinner />
            ) : (
              pageRows.map(({ mapping: { field }, index }) => (
                <Item key={index} data-testid={`custom-mapping-row-for-${field}`}>
                  <SelectGroup>
                    <Field name={`customFieldMappings.${index}.field`} required>
                      {({ field: { name, value, onChange }, meta: { error } }) => (
                        <ProfileFormSelect
                          value={value}
                          onChange={onChange}
                          options={fields}
                          name={name}
                          error={error}
                          placeholder="Select or type field name"
                          allowCreate
                        />
                      )}
                    </Field>
                    <Field name={`customFieldMappings.${index}.type`} required>
                      {({ field: { name, value, onChange }, meta: { error } }) => (
                        <ProfileFormSelect
                          value={value}
                          onChange={onChange}
                          options={fieldTypeOptions}
                          name={name}
                          error={error}
                          placeholder="Select field type"
                          allowCreate={false}
                        />
                      )}
                    </Field>
                  </SelectGroup>
                  {customFieldMappings.length > 1 && (
                    <IconButton name="delete" onClick={() => remove(index)} title="Remove mapping" />
                  )}
                </Item>
              ))
            )}
          </List>
          {totalPages > 1 && (
            <Pagination currentPage={currentPage} totalPages={totalPages} onChange={setMappingsPage} />
          )}
        </>
      )}
    />
  );
};

export default ProfileMappings;
