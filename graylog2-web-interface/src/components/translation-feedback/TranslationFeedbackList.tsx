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
import React, { useMemo } from 'react';
import { useTranslation } from 'react-i18next';

import { Button, ButtonToolbar } from 'components/bootstrap';
import { Link, PaginatedEntityTable, RelativeTime } from 'components/common';
import type { MiddleSectionProps } from 'components/common/PaginatedEntityTable/PaginatedEntityTable';
import type { Sort } from 'stores/PaginationTypes';
import usePermissions from 'hooks/usePermissions';

import { exportTranslationFeedback, fetchTranslationFeedback, keyFn } from './api';
import type { TranslationFeedback } from './types';
import TranslationFeedbackActions from './TranslationFeedbackActions';
import BulkActions from './BulkActions';
import UploadResolutionsButton from './UploadResolutionsButton';

export const DEFAULT_LAYOUT = {
  entityTableId: 'translation-feedback',
  defaultPageSize: 50,
  defaultSort: { attributeId: 'created_at', direction: 'desc' } as Sort,
  defaultDisplayedAttributes: [
    'language',
    'key',
    'text',
    'suggestion',
    'category',
    'reporter',
    'path',
    'status',
    'resolution',
    'created_at',
  ],
  defaultColumnOrder: [
    'language',
    'namespace',
    'key',
    'text',
    'english_text',
    'suggestion',
    'comment',
    'category',
    'reporter',
    'path',
    'status',
    'resolution',
    'created_at',
    'updated_at',
  ],
};

const ExportButtons = ({ searchParams }: MiddleSectionProps) => {
  const { t } = useTranslation('translation_feedback');
  const { isAdmin } = usePermissions();

  return (
    <ButtonToolbar>
      <Button bsSize="small" onClick={() => exportTranslationFeedback('json', searchParams)}>
        {t('list.download_json')}
      </Button>
      <Button bsSize="small" onClick={() => exportTranslationFeedback('csv', searchParams)}>
        {t('list.download_csv')}
      </Button>
      {isAdmin && <UploadResolutionsButton />}
    </ButtonToolbar>
  );
};

const renderActions = (feedback: TranslationFeedback) => <TranslationFeedbackActions feedback={feedback} />;
const bulkSelection = { actions: <BulkActions /> };

const TranslationFeedbackList = () => {
  const { t } = useTranslation('translation_feedback');
  const { isAdmin } = usePermissions();

  const columnRenderers = useMemo(
    () => ({
      attributes: {
        key: {
          renderCell: (key: string, feedback: TranslationFeedback) =>
            key ? <code>{`${feedback.namespace}:${key}`}</code> : <i>{t('list.no_key')}</i>,
        },
        category: { renderCell: (category: string) => t(`categories.${category}`) },
        status: { renderCell: (status: string) => t(`statuses.${status}`) },
        path: { renderCell: (path: string) => (path ? <Link to={path}>{path}</Link> : null) },
        created_at: { renderCell: (createdAt: string) => <RelativeTime dateTime={createdAt} /> },
        updated_at: { renderCell: (updatedAt: string) => <RelativeTime dateTime={updatedAt} /> },
      },
    }),
    [t],
  );

  return (
    <PaginatedEntityTable<TranslationFeedback>
      humanName={t('list.human_name')}
      tableLayout={DEFAULT_LAYOUT}
      fetchEntities={fetchTranslationFeedback}
      keyFn={keyFn}
      entityAttributesAreCamelCase={false}
      columnRenderers={columnRenderers}
      entityActions={isAdmin ? renderActions : undefined}
      bulkSelection={isAdmin ? bulkSelection : undefined}
      middleSection={ExportButtons}
      searchPlaceholder={t('list.search_placeholder')}
    />
  );
};

export default TranslationFeedbackList;
