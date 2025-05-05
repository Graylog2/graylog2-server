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
import type { FormikProps } from 'formik';
import { Formik, Form } from 'formik';
import type { $PropertyType } from 'utility-types';
import styled, { css } from 'styled-components';

import type { GranteesList, CapabilitiesList } from 'logic/permissions/EntityShareState';
import type EntityShareState from 'logic/permissions/EntityShareState';
import type Capability from 'logic/permissions/Capability';
import type Grantee from 'logic/permissions/Grantee';
import { Button } from 'components/bootstrap';

import GranteesSelectorFormGroup from './GranteesSelectorFormGroup';

export type SelectionRequest = {
  granteeId: $PropertyType<Grantee, 'id'>;
  capabilityId: $PropertyType<Capability, 'id'>;
};

export type FormValues = {
  granteeId: $PropertyType<Grantee, 'id'> | undefined;
  capabilityId: $PropertyType<Capability, 'id'>;
};

type Props = {
  availableGrantees: GranteesList;
  availableCapabilities: CapabilitiesList;
  className?: string;
  formRef?: React.Ref<FormikProps<FormValues>>;
  onSubmit: (req: SelectionRequest) => Promise<EntityShareState | null | undefined>;
};

const FormElements = styled.div`
  display: flex;
`;

const Errors = styled.div(
  ({ theme }) => css`
    width: 100%;
    margin-top: 3px;
    color: ${theme.colors.variant.danger};

    > * {
      margin-right: 5px;

      &:last-child {
        margin-right: 0;
      }
    }
  `,
);

const SubmitButton = styled(Button)`
  margin-left: 15px;
`;

const _initialCapabilityId = (capabilities: CapabilitiesList) => {
  const initialCapabilityTitle = 'Viewer';

  return capabilities.find((capability) => capability.title === initialCapabilityTitle)?.id;
};

const GranteesSelector = ({ availableGrantees, availableCapabilities, className = null, onSubmit, formRef = null }: Props) => {
  const initialCapabilityId = _initialCapabilityId(availableCapabilities);

  const _handelSubmit = (data, resetForm) => {
    onSubmit(data).then(() => {
      resetForm();
    });
  };

  return (
    <div className={className}>
      <Formik
        onSubmit={(data, { resetForm }) => _handelSubmit(data, resetForm)}
        innerRef={formRef}
        initialValues={{ granteeId: undefined, capabilityId: initialCapabilityId }}>
        {({ isSubmitting, isValid, errors }) => (
          <Form>
            <FormElements>
              <GranteesSelectorFormGroup availableGrantees={availableGrantees} availableCapabilities={availableCapabilities} />
              <SubmitButton
                bsStyle="success"
                disabled={isSubmitting || !isValid}
                title="Add Collaborator"
                type="submit">
                Add Collaborator
              </SubmitButton>
            </FormElements>
            {errors && (
              <Errors>
                {Object.entries(errors).map(([fieldKey, value]: [string, unknown]) => (
                  <span key={fieldKey}>{String(value)}.</span>
                ))}
              </Errors>
            )}
          </Form>
        )}
      </Formik>
    </div>
  );
};

export default GranteesSelector;
