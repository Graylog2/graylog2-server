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

import ErrorPage from 'components/errors/ErrorPage';
import useProductName from 'brand-customization/useProductName';
import Center from 'components/common/Center';

type PageErrorOverviewProps = {
  /** Array of errors that prevented the original page to load. */
  error: Error;
  title?: string;
};

/**
 * Component that renders a page when there was an error and certain information can't be fetched. Use it
 * only when the page would make no sense if the information is not available (i.e. a node page where we
 * can't reach the node).
 */
const PageErrorOverview = ({ error, title = undefined }: PageErrorOverviewProps) => {
  const { t } = useTranslation('common');
  const productName = useProductName();
  const description = <p>{t('errors.page_error_description')}</p>;

  return (
    <ErrorPage title={title ?? t('errors.page_error_title')} description={description} displayPageLayout={false}>
      <pre>{error.toString()}</pre>
      <Center>{t('errors.page_error_logs', { productName })}</Center>
    </ErrorPage>
  );
};

export default PageErrorOverview;
