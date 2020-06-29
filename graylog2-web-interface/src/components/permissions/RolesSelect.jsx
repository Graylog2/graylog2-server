// @flow strict
import * as React from 'react';
import { useCallback } from 'react';
import { Field } from 'formik';

import { Select } from 'components/common';
import type { AvailableRoles } from 'logic/permissions/EntityShareState';
import type { Role } from 'logic/permissions/types';

const _rolesOptions = (roles: AvailableRoles) => (
  roles.map((role) => (
    { label: role.title, value: role.id }
  )).toJS()
);

type Props = {
  roles: AvailableRoles,
  onChange?: $PropertyType<Role, 'id'> => void,
};

const RolesSelect = ({ roles, onChange, ...rest }: Props) => {
  const rolesOptions = _rolesOptions(roles);

  const handleChange = useCallback((name, roleId, onFieldChange) => {
    onFieldChange({ target: { value: roleId, name } });

    if (typeof onChange === 'function') {
      onChange(roleId);
    }
  }, [onChange]);

  return (
    <Field name="roleId">
      {({ field: { name, value, onChange: onFieldChange } }) => (
        <Select {...rest}
                placeholder="Select a role"
                options={rolesOptions}
                clearable={false}
                onChange={(roleId) => handleChange(name, roleId, onFieldChange)}
                value={value} />
      )}
    </Field>
  );
};

RolesSelect.defaultProps = {
  onChange: undefined,
};

export default RolesSelect;
