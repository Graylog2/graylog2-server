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

import { Badge, Label } from 'components/bootstrap';
import type { ColumnRenderers } from 'components/common/EntityDataTable';

import { SOURCE_TYPE_LABELS } from './Constants';

import type { Source } from '../types';

const SourceTypeLabel = ({ type }: { type: string }) => {
  const { t } = useTranslation('collectors');

  return <Label bsStyle="info">{SOURCE_TYPE_LABELS[type] ? t(SOURCE_TYPE_LABELS[type]) : type}</Label>;
};

const EnabledBadge = ({ enabled }: { enabled: boolean }) => {
  const { t } = useTranslation('collectors');

  return (
    <Badge color={enabled ? 'success' : 'warning'} variant="light">
      {enabled ? t('source_columns.enabled') : t('source_status.disabled')}
    </Badge>
  );
};

const customColumnRenderers = (): ColumnRenderers<Source> => ({
  attributes: {
    name: {
      renderCell: (name: string) => <strong>{name}</strong>,
    },
    type: {
      renderCell: (type: string) => <SourceTypeLabel type={type} />,
      staticWidth: 190,
    },
    enabled: {
      renderCell: (_enabled: boolean, source: Source) => <EnabledBadge enabled={source.enabled} />,
      staticWidth: 110,
    },
    description: {
      renderCell: (description: string) => <span>{description || '—'}</span>,
    },
  },
});

export default customColumnRenderers;
