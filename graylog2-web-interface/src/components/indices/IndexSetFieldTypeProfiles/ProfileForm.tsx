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
import { Formik, Form } from 'formik';
import countBy from 'lodash/countBy';

import type { IndexSetFieldTypeProfileForm } from 'components/indices/IndexSetFieldTypeProfiles/types';
import { FormikInput, FormSubmit, InputOptionalInfo } from 'components/common';
import { Col } from 'components/bootstrap';
import useFieldTypes from 'views/logic/fieldtypes/useFieldTypes';
import useFieldTypesForMapping from 'views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypesForMappings';
import { defaultCompare } from 'logic/DefaultCompare';
import isReservedField from 'views/logic/IsReservedField';
import { sortMappingsByField } from 'components/indices/IndexSetFieldTypeProfiles/helpers/mappingsPaging';
import ProfileMappings from 'components/indices/IndexSetFieldTypeProfiles/ProfileMappings';

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

const ProfileForm = ({
  initialValues = { name: '', description: '', customFieldMappings: [{ type: '', field: '' }] },
  submitButtonText,
  submitLoadingText,
  onCancel,
  onSubmit,
}: Props) => {
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
        {({ isSubmitting, isValid, isValidating }) => (
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
            <ProfileMappings
              fields={fields}
              fieldTypeOptions={fieldTypeOptions}
              isLoading={isLoading || isLoadingFieldTypes}
            />
            <StyledFormSubmit
              submitButtonText={submitButtonText}
              onCancel={onCancel}
              disabledSubmit={isValidating || !isValid}
              isSubmitting={isSubmitting}
              submitLoadingText={submitLoadingText}
            />
          </Form>
        )}
      </Formik>
    </Col>
  );
};

export default ProfileForm;
