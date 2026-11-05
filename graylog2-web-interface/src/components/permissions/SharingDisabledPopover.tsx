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

import HoverForHelp from 'components/common/HoverForHelp';

type Props = {
  type: string;
  description?: string;
};

const SharingDisabledPopover = ({ type, description = undefined }: Props) => {
  const { t } = useTranslation('users');
  const getReadableType = (_type: string) => _type.replaceAll('_', ' ');

  return (
    <HoverForHelp title={t('permissions.sharing_disabled.title')} pullRight={false}>
      {description || t('permissions.sharing_disabled.owners_only', { type: getReadableType(type) })}
    </HoverForHelp>
  );
};

export default SharingDisabledPopover;
