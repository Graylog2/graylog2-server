// @flow strict
import * as React from 'react';
import { useCallback } from 'react';
import { Field } from 'formik';

import { Select } from 'components/common';
import type { AvailableCapabilities } from 'logic/permissions/EntityShareState';
import type { Capability } from 'logic/permissions/types';

const _capabilitiesOptions = (capabilities: AvailableCapabilities) => (
  capabilities.map((capability) => (
    { label: capability.title, value: capability.id }
  )).toJS()
);

type Props = {
  onChange?: $PropertyType<Capability, 'id'> => void,
  capabilities: AvailableCapabilities,
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
