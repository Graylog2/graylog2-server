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

import type { IndexSetFieldTypeProfile } from 'components/indices/IndexSetFiledTypeProfiles/types';
import { FormikInput, IconButton, Select, ModalSubmit } from 'components/common';
import { Button, HelpBlock, Modal } from 'components/bootstrap';
import useFieldTypes from 'views/logic/fieldtypes/useFieldTypes';
import useFieldTypesForMapping from 'views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypes';
import { defaultCompare } from 'logic/DefaultCompare';

const StyledSelect = styled(Select)`
  width: 400px;
  margin-bottom: 20px;
`;

const StyledLabel = styled.h5`
  font-weight: bold;
  margin-bottom: 5px;
`;

const Item = styled.div`
  display: flex;
  gap: 5px;
`;
type Props = {
  initialValues?: IndexSetFieldTypeProfile,
  submitButtonText: string,
  onCancel: () => void,
  title: string,
  onSubmit: (profile: IndexSetFieldTypeProfile) => void
}

const ProfileModalForm = ({ initialValues, submitButtonText, title, onCancel, onSubmit }: Props) => {
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
    <Formik<IndexSetFieldTypeProfile> initialValues={initialValues}
                                      onSubmit={_onSubmit}>
      {({ isSubmitting, isValidating, values: { customFieldMappings } }) => (
        <Form>
          <Modal.Header closeButton>
            <Modal.Title>{title}</Modal.Title>
          </Modal.Header>
          <Modal.Body>
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
                            {customFieldMappings.map((_, index) => (
                              // eslint-disable-next-line react/no-array-index-key
                              <Item key={index}>
                                <Field name={`customFieldMappings.${index}.field`} required>
                                  {({ field: { name, value, onChange } }) => (
                                    <StyledSelect options={fields}
                                                  value={value}
                                                  name={name}
                                                  inputId={name}
                                                  onChange={(newVal) => {
                                                    onChange({ target: { value: newVal, name } });
                                                  }}
                                                  placeholder="Select or type the field"
                                                  disabled={isLoading}
                                                  required
                                                  allowCreate />
                                  )}
                                </Field>
                                <Field name={`customFieldMappings.${index}.type`} required>
                                  {({ field: { name, value, onChange } }) => (
                                    <StyledSelect options={fieldTypeOptions}
                                                  value={value}
                                                  name={name}
                                                  inputId={name}
                                                  onChange={(newVal) => {
                                                    onChange({ target: { value: newVal, name } });
                                                  }}
                                                  placeholder="Select field type"
                                                  disabled={isLoadingFieldTypes}
                                                  required />
                                  )}
                                </Field>
                                {(customFieldMappings.length > 1) && <IconButton name="trash-alt" onClick={() => (remove(index))} title="Remove mapping" />}
                              </Item>
                            ))}
                            <Button bsSize="xs" onClick={() => push({})} name="plus" title="Add mapping">Add mapping</Button>
                          </>
                        )} />
          </Modal.Body>
          <Modal.Footer>
            <ModalSubmit submitButtonText={submitButtonText}
                         onCancel={onCancel}
                         disabledSubmit={isValidating}
                         isSubmitting={isSubmitting} />
          </Modal.Footer>
        </Form>
      )}
    </Formik>
  );
};

ProfileModalForm.defaultProps = {
  initialValues: { name: '', description: '', customFieldMappings: [{ type: '', field: '' }] },
};

export default ProfileModalForm;
