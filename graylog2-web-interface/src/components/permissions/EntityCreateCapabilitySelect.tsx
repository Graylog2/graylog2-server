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
import { useTranslation } from 'react-i18next';

import { Select } from 'components/common';
import type { CapabilityType } from 'logic/permissions/types';
import capabilityLabel from 'logic/permissions/capabilityLabel';
import type { CapabilitiesList } from 'logic/permissions/EntityShareState';

type Props = {
  onChange: (id: CapabilityType['id']) => void;
  capabilities: CapabilitiesList;
  title?: string;
  value: CapabilityType['id'];
};

const _capabilitiesOptions = (capabilities: CapabilitiesList) =>
  capabilities.map((capability) => ({ label: capabilityLabel(capability.title), value: capability.id })).toJS();

const EntityCreateCapabilitySelect = ({
  capabilities,
  onChange,
  value,
  title = undefined,
  ...rest
}: Props) => {
  const { t } = useTranslation('users');
  const capabilitiesOptions = _capabilitiesOptions(capabilities);

  return (
    <Select
      clearable={false}
      onChange={onChange}
      options={capabilitiesOptions}
      placeholder={title ?? t('permissions.select_capability')}
      value={value}
      {...rest}
    />
  );
};

export default EntityCreateCapabilitySelect;
