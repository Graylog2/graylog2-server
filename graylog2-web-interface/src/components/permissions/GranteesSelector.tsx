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
import { Formik, FormikProps, Form, Field } from 'formik';
import { $PropertyType } from 'utility-types';
import styled, { css } from 'styled-components';

import EntityShareState, { GranteesList, CapabilitiesList } from 'logic/permissions/EntityShareState';
import Capability from 'logic/permissions/Capability';
import Grantee from 'logic/permissions/Grantee';
import { Button } from 'components/graylog';
import { Select } from 'components/common';
import SelectGroup from 'components/common/SelectGroup';

import GranteeIcon from './GranteeIcon';
import CapabilitySelect from './CapabilitySelect';

export type SelectionRequest = {
  granteeId: $PropertyType<Grantee, 'id'>,
  capabilityId: $PropertyType<Capability, 'id'>,
};

export type FormValues = {
  granteeId: $PropertyType<Grantee, 'id'> | undefined,
  capabilityId: $PropertyType<Capability, 'id'>,
}

type Props = {
  availableGrantees: GranteesList,
  availableCapabilities: CapabilitiesList,
  className?: string,
  formRef?: React.Ref<FormikProps<FormValues>>,
  onSubmit: (req: SelectionRequest) => Promise<EntityShareState | null | undefined>,
};

const FormElements = styled.div`
  display: flex;
`;

const Errors = styled.div(({ theme }) => css`
  width: 100%;
  margin-top: 3px;
  color: ${theme.colors.variant.danger};

  > * {
    margin-right: 5px;

    :last-child {
      margin-right: 0;
    }
  }
`);

const GranteesSelect = styled(Select)`
  flex: 1;
`;

const GranteesSelectOption = styled.div`
  display: flex;
  align-items: center;
`;

const StyledGranteeIcon = styled(GranteeIcon)`
  margin-right: 5px;
`;

const StyledSelectGroup = styled(SelectGroup)`
  flex: 1;

  > div:last-child {
    flex: 0.5;
  }
`;

const SubmitButton = styled(Button)`
  margin-left: 15px;
`;

const _granteesOptions = (grantees: GranteesList) => {
  return grantees.map((grantee) => (
    { label: grantee.title, value: grantee.id, granteeType: grantee.type }
  )).toJS();
};

const _initialCapabilityId = (capabilities: CapabilitiesList) => {
  const initialCapabilityTitle = 'Viewer';

  return capabilities.find((capability) => capability.title === initialCapabilityTitle)?.id;
};

const _isRequired = (field) => (value) => (!value ? `The ${field} is required` : undefined);

const _renderGranteesSelectOption = ({ label, granteeType }: {label: string, granteeType: $PropertyType<Grantee, 'type'> }) => (
  <GranteesSelectOption>
    <StyledGranteeIcon type={granteeType} />
    {label}
  </GranteesSelectOption>
);

const GranteesSelector = ({ availableGrantees, availableCapabilities, className, onSubmit, formRef }: Props) => {
  const granteesOptions = _granteesOptions(availableGrantees);
  const initialCapabilityId = _initialCapabilityId(availableCapabilities);

  const _handelSubmit = (data, resetForm) => {
    onSubmit(data).then(() => { resetForm(); });
  };

  return (
    <div className={className}>
      <Formik onSubmit={(data, { resetForm }) => _handelSubmit(data, resetForm)}
              innerRef={formRef}
              initialValues={{ granteeId: undefined, capabilityId: initialCapabilityId }}>
        {({ isSubmitting, isValid, errors }) => (
          <Form>
            <FormElements>
              <StyledSelectGroup>
                <Field name="granteeId" validate={_isRequired('grantee')}>
                  {({ field: { name, value, onChange } }) => (
                    <GranteesSelect inputProps={{ 'aria-label': 'Search for users and teams' }}
                                    onChange={(granteeId) => onChange({ target: { value: granteeId, name } })}
                                    optionRenderer={_renderGranteesSelectOption}
                                    options={granteesOptions}
                                    placeholder="Search for users and teams"
                                    value={value} />
                  )}
                </Field>
                <CapabilitySelect capabilities={availableCapabilities} />
              </StyledSelectGroup>
              <SubmitButton bsStyle="success"
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

GranteesSelector.defaultProps = {
  className: undefined,
  formRef: undefined,
};

export default GranteesSelector;
