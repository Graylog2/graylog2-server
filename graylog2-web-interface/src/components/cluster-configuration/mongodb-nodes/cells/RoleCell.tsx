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
import React from 'react';
import { useTranslation } from 'react-i18next';

import { Label } from 'components/bootstrap';

import { SecondaryText } from '../../shared-components/NodeMetricsLayout';
import { MongodbRole, type MongodbRoleType } from '../fetchClusterMongodbNodes';

const ROLE_LABEL_KEYS: Record<MongodbRoleType, string> = {
  [MongodbRole.PRIMARY]: 'mongodb_nodes.roles.primary',
  [MongodbRole.SECONDARY]: 'mongodb_nodes.roles.secondary',
  [MongodbRole.ARBITER]: 'mongodb_nodes.roles.arbiter',
  [MongodbRole.STANDALONE]: 'mongodb_nodes.roles.standalone',
};

type Props = {
  role: string | undefined | null;
};

const RoleCell = ({ role }: Props) => {
  const { t } = useTranslation('system');
  const upperRole = role?.toUpperCase();

  if (!upperRole) {
    return (
      <SecondaryText>
        <span>{t('shared.not_available')}</span>
      </SecondaryText>
    );
  }

  const labelKey = ROLE_LABEL_KEYS[upperRole as MongodbRoleType];

  // Roles unknown to the frontend are shown as the server sends them.
  return <Label bsSize="xs">{labelKey ? t(labelKey) : upperRole}</Label>;
};

export default RoleCell;
