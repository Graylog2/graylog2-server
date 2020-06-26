// @flow strict
import * as React from 'react';
import { Field } from 'formik';

import { Select } from 'components/common';
import type { AvailableRoles } from 'logic/permissions/EntityShareState';

const _rolesOptions = (roles: AvailableRoles) => (
  roles.map((role) => (
    { label: role.title, value: role.id }
  )).toJS()
);

type Props = {
  roles: AvailableRoles,
};

const RolesSelect = ({ roles, ...rest }: Props) => {
  const rolesOptions = _rolesOptions(roles);

  return (
    <Field name="roleId">
      {({ field: { name, value, onChange } }) => (
        <Select {...rest}
                placeholder="Select a role"
                options={rolesOptions}
                clearable={false}
                onChange={(roleId) => onChange({ target: { value: roleId, name } })}
                value={value} />
      )}
    </Field>
  );
};

export default RolesSelect;
