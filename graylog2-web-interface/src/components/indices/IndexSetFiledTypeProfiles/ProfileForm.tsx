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
import React, { useMemo } from 'react';
import { Formik, Form, FieldArray, Field } from 'formik';
import countBy from 'lodash/countBy';
import { Loader } from '@mantine/core';

import type { IndexSetFieldTypeProfile } from 'components/indices/IndexSetFiledTypeProfiles/types';
import { FormikInput, IconButton, Select, FormSubmit } from 'components/common';
import { Button, Col, HelpBlock, Input } from 'components/bootstrap';
import useFieldTypes from 'views/logic/fieldtypes/useFieldTypes';
import useFieldTypesForMapping from 'views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypesForMappings';
import { defaultCompare } from 'logic/DefaultCompare';

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
  gap: 15px;
  margin-bottom: 15px;
`;
const StyledLabel = styled.h5`
  font-weight: bold;
  margin-bottom: 5px;
`;

const Item = styled.div`
  display: flex;
  gap: 5px;
  align-items: center;
`;

const StyledFormSubmit = styled(FormSubmit)`
  margin-top: 15px;
`;
type Props = {
  initialValues?: IndexSetFieldTypeProfile,
  submitButtonText: string,
  onCancel: () => void,
  onSubmit: (profile: IndexSetFieldTypeProfile) => void
}

const getFieldError = (field: string, occurrences: number) => {
  if (!field) return 'Filed is required';
  if (occurrences > 1) return 'This field occurs several times';

  return undefined;
};

const validate = (formValues: IndexSetFieldTypeProfile) => {
  const errors: { name?: string, customFieldMappings?: Array<{ field?: string, type?: string }>} = {};

  if (!formValues.name) {
    errors.name = 'Profile name is required';
  }

  const fieldsOccurrences = countBy(formValues.customFieldMappings, 'field');

  const customFieldMappings: Array<{ field: string, type: string }> = formValues
    .customFieldMappings
    .map(({ field, type }) => {
      if (field && type && (fieldsOccurrences[field] === 1)) return undefined;

      return ({
        field: getFieldError(field, fieldsOccurrences[field]),
        type: !type ? 'Type is required' : undefined,
      });
    });

  if (customFieldMappings.filter((item) => item).length > 0) {
    errors.customFieldMappings = customFieldMappings;
  }

  return errors;
};

type ProfileFormSelectProps = {
  onChange: (param: { target: { value: string, name: string } }) => void,
  options: Array<{ value: string, label: string, disabled?: boolean }>,
  error: string,
  name: string,
  value: string | undefined | null,
  placeholder: string,
  allowCreate: boolean,
}

const ProfileFormSelect = ({ onChange, options, error, name, value, placeholder, allowCreate }: ProfileFormSelectProps) => (
  <SelectContainer>
    <Input error={error} name={name} id={name}>
      <Select options={options}
              value={value}
              inputId={name}
              onChange={(newVal) => {
                onChange({ target: { value: newVal, name } });
              }}
              inputProps={{ 'aria-label': `Select ${name}` }}
              placeholder={placeholder}
              allowCreate={allowCreate} />
    </Input>
  </SelectContainer>
);

const ProfileForm = ({ initialValues, submitButtonText, onCancel, onSubmit }: Props) => {
  const { data, isLoading } = useFieldTypes(undefined, undefined);
  const { data: { fieldTypes }, isLoading: isLoadingFieldTypes } = useFieldTypesForMapping();
  const fieldTypeOptions = useMemo(() => Object.entries(fieldTypes)
    .sort(([, label1], [, label2]) => defaultCompare(label1, label2))
    .map(([value, label]) => ({
      value,
      label,
    })), [fieldTypes]);
  const fields = useMemo(() => (isLoading ? [] : data.map(({ value: { name } }) => ({ value: name, label: name }))), [data, isLoading]);

  const _onSubmit = (profile: IndexSetFieldTypeProfile) => {
    onSubmit(profile);
  };

  return (
    <Col lg={8}>
      <Formik<IndexSetFieldTypeProfile> initialValues={initialValues}
                                        onSubmit={_onSubmit}
                                        validate={validate}
                                        validateOnChange>
        {({ isSubmitting, isValidating, values: { customFieldMappings } }) => (
          <Form>
            <FormikInput name="name"
                         label="Profile name"
                         id="index-set-field-type-profile-name"
                         placeholder="Type a profile name"
                         help="A descriptive name of the new profile"
                         required />
            <FormikInput name="description"
                         id="index-set-field-type-profile-description"
                         placeholder="Type a profile description"
                         label="Description"
                         type="textarea"
                         help="Longer description for Profile"
                         rows={6} />
            <FieldArray name="customFieldMappings"
                        render={({ remove, push }) => (
                          <>
                            <StyledLabel>Setup mappings</StyledLabel>
                            <HelpBlock>
                              Here you can setup type mapping to any filed. If the needed field is not exist on the list you can type it and create
                            </HelpBlock>
                            <List>
                              {(isLoading || isLoadingFieldTypes) ? <Loader /> : customFieldMappings.map((_, index) => (
                                // eslint-disable-next-line react/no-array-index-key
                                <Item key={index}>
                                  <SelectGroup>
                                    <Field name={`customFieldMappings.${index}.field`} required>
                                      {({ field: { name, value, onChange }, meta: { error } }) => (
                                        <ProfileFormSelect value={value}
                                                           onChange={onChange}
                                                           options={fields}
                                                           name={name}
                                                           error={error}
                                                           placeholder="Select ot type field name"
                                                           allowCreate />
                                      )}
                                    </Field>
                                    <Field name={`customFieldMappings.${index}.type`} required>
                                      {({ field: { name, value, onChange }, meta: { error } }) => (
                                        <ProfileFormSelect value={value}
                                                           onChange={onChange}
                                                           options={fieldTypeOptions}
                                                           name={name}
                                                           error={error}
                                                           placeholder="Select field type"
                                                           allowCreate={false} />
                                      )}
                                    </Field>
                                  </SelectGroup>
                                  {(customFieldMappings.length > 1) && <IconButton name="trash-alt" onClick={() => (remove(index))} title="Remove mapping" />}
                                </Item>
                              ))}
                            </List>
                            <Button bsSize="xs" onClick={() => push({})} name="plus" title="Add mapping">Add mapping</Button>
                          </>
                        )} />
            <StyledFormSubmit submitButtonText={submitButtonText}
                              onCancel={onCancel}
                              disabledSubmit={isValidating}
                              isSubmitting={isSubmitting} />
          </Form>
        )}
      </Formik>
    </Col>
  );
};

ProfileForm.defaultProps = {
  initialValues: { name: '', description: '', customFieldMappings: [{ type: '', field: '' }] },
};

export default ProfileForm;
