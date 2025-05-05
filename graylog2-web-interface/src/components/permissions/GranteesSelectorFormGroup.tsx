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
import { Field } from 'formik';
import type { $PropertyType } from 'utility-types';
import styled from 'styled-components';

import type { CapabilitiesList, GranteesList } from 'logic/permissions/EntityShareState';
import { Select } from 'components/common';
import SelectGroup from 'components/common/SelectGroup';
import type Grantee from 'logic/permissions/Grantee';

import CapabilitySelect from './CapabilitySelect';
import { StyledGranteeIcon } from './CommonStyledComponents';

type Props = {
  availableGrantees: GranteesList;
  availableCapabilities: CapabilitiesList;
};

const GranteesSelect = styled(Select)`
  flex: 1;
`;

const GranteesSelectOption = styled.div`
  display: flex;
  align-items: center;
`;

const StyledSelectGroup = styled(SelectGroup)`
  flex: 1;

  > div:last-child {
    flex: 0.5;
  }
`;

const _granteesOptions = (grantees: GranteesList) =>
  grantees.map((grantee) => ({ label: grantee.title, value: grantee.id, granteeType: grantee.type })).toJS();

const _isRequired = (field) => (value) => (!value ? `The ${field} is required` : undefined);

const _renderGranteesSelectOption = ({
  label,
  granteeType,
}: {
  label: string;
  granteeType: $PropertyType<Grantee, 'type'>;
}) => (
  <GranteesSelectOption>
    <StyledGranteeIcon type={granteeType} />
    {label}
  </GranteesSelectOption>
);
const GranteesSelectorFormGroup = ({ availableGrantees, availableCapabilities }: Props) => {
  const granteesOptions = _granteesOptions(availableGrantees);

  return (
      <StyledSelectGroup>
        <Field name="granteeId" validate={_isRequired('grantee')}>
          {({ field: { name, value, onChange } }) => (
            <GranteesSelect
              onChange={(granteeId) => onChange({ target: { value: granteeId, name } })}
              optionRenderer={_renderGranteesSelectOption}
              options={granteesOptions}
              placeholder="Search for users and teams"
              value={value}
            />
          )}
        </Field>
        <CapabilitySelect capabilities={availableCapabilities} />
      </StyledSelectGroup>
   );
};

export default GranteesSelectorFormGroup;
