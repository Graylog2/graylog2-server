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
import { $PropertyType } from 'utility-types';
import { useCallback } from 'react';
import { Field } from 'formik';

import { Select } from 'components/common';
import type { CapabilitiesList } from 'logic/permissions/EntityShareState';
import type { CapabilityType } from 'logic/permissions/types';

const _capabilitiesOptions = (capabilities: CapabilitiesList) => (
  capabilities.map((capability) => (
    { label: capability.title, value: capability.id }
  )).toJS()
);

type Props = {
  onChange?: (id: $PropertyType<CapabilityType, 'id'>) => void,
  capabilities: CapabilitiesList,
  title?: string,
};

const CapabilitySelect = ({ capabilities, onChange, title, ...rest }: Props) => {
  const capabilitiesOptions = _capabilitiesOptions(capabilities);

  const handleChange = useCallback((name, capabilityId, onFieldChange) => {
    onFieldChange({ target: { value: capabilityId, name } });

    if (typeof onChange === 'function') {
      onChange(capabilityId);
    }
  }, [onChange]);

  return (
    <Field name="capabilityId">
      {({ field: { name, value, onChange: onFieldChange } }) => (
        <Select {...rest}
                clearable={false}
                inputProps={{ 'aria-label': title }}
                onChange={(capabilityId) => handleChange(name, capabilityId, onFieldChange)}
                options={capabilitiesOptions}
                placeholder={title}
                value={value} />
      )}
    </Field>
  );
};

CapabilitySelect.defaultProps = {
  onChange: undefined,
  title: 'Select a capability',
};

export default CapabilitySelect;
