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
import React, { useMemo, useState } from 'react';
import { Formik, Form, FieldArray, Field } from 'formik';
import countBy from 'lodash/countBy';

import type { IndexSetFieldTypeProfileForm } from 'components/indices/IndexSetFieldTypeProfiles/types';
import { FormikInput, IconButton, Select, FormSubmit, Spinner, InputOptionalInfo, Pagination } from 'components/common';
import { Alert, Button, Col, HelpBlock, Input } from 'components/bootstrap';
import useFieldTypes from 'views/logic/fieldtypes/useFieldTypes';
import useFieldTypesForMapping from 'views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypesForMappings';
import { defaultCompare } from 'logic/DefaultCompare';
import isReservedField from 'views/logic/IsReservedField';
import {
  MAPPINGS_PAGE_SIZE,
  getLastPage,
  getPageOfRow,
  getRowIndexesWithErrors,
  getRowsMatchingSearch,
  sortMappingsByField,
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

const StyledFormSubmit = styled(FormSubmit)`
  margin-top: 30px;
`;
type Props = {
  initialValues?: IndexSetFieldTypeProfileForm;
  submitButtonText: string;
  submitLoadingText: string;
  onCancel: () => void;
  onSubmit: (profile: IndexSetFieldTypeProfileForm) => void;
};

const getFieldError = (field: string, occurrences: number) => {
  if (!field) return 'Field is required';
  if (isReservedField(field)) return 'Field is reserved';
  if (occurrences > 1) return 'This field occurs several times';

  return undefined;
};

const validate = (formValues: IndexSetFieldTypeProfileForm) => {
  const errors: { name?: string; customFieldMappings?: Array<{ field?: string; type?: string }> } = {};

  if (!formValues.name) {
    errors.name = 'Profile name is required';
  }

  const fieldsOccurrences = countBy(formValues.customFieldMappings, 'field');

  const customFieldMappings: Array<{ field: string; type: string }> = formValues.customFieldMappings.map(
    ({ field, type }) => {
      if (field && type && fieldsOccurrences[field] === 1) return undefined;

      return {
        field: getFieldError(field, fieldsOccurrences[field]),
        type: !type ? 'Type is required' : undefined,
      };
    },
  );

  if (customFieldMappings.filter((item) => item).length > 0) {
    errors.customFieldMappings = customFieldMappings;
  }

  return errors;
};

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

const ProfileForm = ({
  initialValues = { name: '', description: '', customFieldMappings: [{ type: '', field: '' }] },
  submitButtonText,
  submitLoadingText,
  onCancel,
  onSubmit,
}: Props) => {
  const [mappingsPage, setMappingsPage] = useState(1);
  const [mappingsSearch, setMappingsSearch] = useState('');
  const { data, isLoading } = useFieldTypes(undefined, undefined);
  const {
    data: { fieldTypes },
    isLoading: isLoadingFieldTypes,
  } = useFieldTypesForMapping();
  const fieldTypeOptions = useMemo(
    () =>
      Object.entries(fieldTypes)
        .sort(([, label1], [, label2]) => defaultCompare(label1, label2))
        .map(([value, label]) => ({
          value,
          label,
        })),
    [fieldTypes],
  );
  const fields = useMemo(
    () =>
      isLoading
        ? []
        : data.map(({ value: { name } }) => ({ value: name, label: name, disabled: isReservedField(name) })),
    [data, isLoading],
  );

  const _onSubmit = (profile: IndexSetFieldTypeProfileForm) => {
    onSubmit(profile);
  };

  return (
    <Col lg={8}>
      <Formik<IndexSetFieldTypeProfileForm>
        initialValues={{
          ...initialValues,
          customFieldMappings: sortMappingsByField(initialValues.customFieldMappings),
        }}
        onSubmit={_onSubmit}
        validate={validate}
        validateOnChange>
        {({ isSubmitting, isValid, isValidating, errors, values: { customFieldMappings } }) => {
          const isPaginated = customFieldMappings.length > MAPPINGS_PAGE_SIZE;
          const matchingRows = getRowsMatchingSearch(customFieldMappings, isPaginated ? mappingsSearch : '');
          const totalPages = getLastPage(matchingRows.length);
          const currentPage = Math.min(mappingsPage, totalPages);
          const firstRowPosition = (currentPage - 1) * MAPPINGS_PAGE_SIZE;
          const pageRows = matchingRows.slice(firstRowPosition, firstRowPosition + MAPPINGS_PAGE_SIZE);
          const rowsWithErrors = getRowIndexesWithErrors(errors.customFieldMappings);

          return (
            <Form>
              <FormikInput
                name="name"
                label="Profile name"
                id="index-set-field-type-profile-name"
                placeholder="Type a profile name"
                help="A descriptive name of the new profile"
                required
              />
              <FormikInput
                name="description"
                id="index-set-field-type-profile-description"
                placeholder="Type a profile description"
                label={
                  <>
                    Description <InputOptionalInfo />
                  </>
                }
                type="textarea"
                help="Longer description for profile"
                rows={6}
              />
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
                        {rowsWithErrors.length} {rowsWithErrors.length === 1 ? 'mapping has' : 'mappings have'} errors.{' '}
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
                      {isLoading || isLoadingFieldTypes ? (
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
              <StyledFormSubmit
                submitButtonText={submitButtonText}
                onCancel={onCancel}
                disabledSubmit={isValidating || !isValid}
                isSubmitting={isSubmitting}
                submitLoadingText={submitLoadingText}
              />
            </Form>
          );
        }}
      </Formik>
    </Col>
  );
};

export default ProfileForm;
