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

import type FetchError from 'logic/errors/FetchError';

import UnauthorizedErrorPage from './UnauthorizedErrorPage';

type Props = {
  error: FetchError | undefined | null;
  missingStreamIds?: string[];
};

const StreamPermissionErrorPage = ({ error, missingStreamIds = [] }: Props) => {
  const { t } = useTranslation('streams');
  const description = (
    <>
      <p>{t('stream_permission_error_page.description_p1')}</p>
      <p>{t('stream_permission_error_page.description_p2')}</p>
    </>
  );
  const streamIds = missingStreamIds.length > 0 ? missingStreamIds : error?.additional?.body?.streams;
  const errorDetails =
    streamIds?.length > 0 ? t('stream_permission_error_page.error_details', { ids: streamIds.join(', ') }) : undefined;

  return (
    <UnauthorizedErrorPage
      error={error}
      description={description}
      title={t('stream_permission_error_page.title')}
      errorDetails={errorDetails}
    />
  );
};

export default StreamPermissionErrorPage;
