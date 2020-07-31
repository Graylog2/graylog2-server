// @flow strict
import * as React from 'react';
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
  onChange?: $PropertyType<CapabilityType, 'id'> => void,
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
