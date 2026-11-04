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
import React, { useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';

import { Button } from 'components/bootstrap';
import { useTableFetchContext } from 'components/common/PaginatedEntityTable';
import UserNotification from 'util/UserNotification';

import type { Resolutions } from './api';
import { applyTranslationFeedbackResolutions } from './api';

const isResolutions = (value: unknown): value is Resolutions =>
  typeof value === 'object' &&
  value !== null &&
  Array.isArray((value as Resolutions).resolutions) &&
  (value as Resolutions).resolutions.every((resolution) => typeof resolution?.id === 'string');

const readText = (file: File) =>
  new Promise<string>((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result as string);
    reader.onerror = () => reject(reader.error);
    reader.readAsText(file);
  });

/**
 * Uploads a file with the status and a note for the reporter of each handled report, e.g.
 * `{ "resolutions": [{ "id": "...", "status": "fixed", "note": "Changed to „Suche“" }] }`.
 */
const UploadResolutionsButton = () => {
  const { t } = useTranslation('translation_feedback');
  const { refetch } = useTableFetchContext();
  const input = useRef<HTMLInputElement>(null);
  const [isUploading, setIsUploading] = useState(false);

  const onError = (error: unknown) => UserNotification.error(t('upload.failed', { error }), t('upload.failed_title'));

  const onFile = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    // Lets the same file be chosen again, e.g. after fixing it.
    // eslint-disable-next-line no-param-reassign
    event.target.value = '';

    if (!file) {
      return;
    }

    let resolutions: unknown;

    try {
      resolutions = JSON.parse(await readText(file));
    } catch (error) {
      onError(error);

      return;
    }

    if (!isResolutions(resolutions)) {
      onError(t('upload.invalid_file'));

      return;
    }

    setIsUploading(true);

    applyTranslationFeedbackResolutions(resolutions)
      .then(({ changed, not_found: notFound }) => {
        UserNotification.success(
          notFound > 0
            ? t('upload.applied_with_missing', { count: changed, notFound })
            : t('upload.applied', { count: changed }),
          t('upload.applied_title'),
        );
        refetch();
      })
      .catch(onError)
      .finally(() => setIsUploading(false));
  };

  return (
    <>
      <input
        ref={input}
        type="file"
        accept="application/json,.json"
        hidden
        aria-label={t('upload.button')}
        onChange={onFile}
        data-testid="upload-resolutions-input"
      />
      <Button bsSize="small" onClick={() => input.current?.click()} disabled={isUploading} title={t('upload.help')}>
        {isUploading ? t('upload.uploading') : t('upload.button')}
      </Button>
    </>
  );
};

export default UploadResolutionsButton;
