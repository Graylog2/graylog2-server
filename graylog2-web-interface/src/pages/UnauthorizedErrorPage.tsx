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

import type FetchError from 'logic/errors/FetchError';
import { Icon, ClipboardButton } from 'components/common';
import ErrorPage from 'components/errors/ErrorPage';
import withLocation from 'routing/withLocation';
import type { Location } from 'routing/withLocation';

const createErrorMessageString = (
  errorDetails: string | null | undefined,
  pageDetails: string,
  errorMessage: string | undefined,
) => [errorDetails, pageDetails, errorMessage].filter((part) => !!part).join('\n');

type Props = {
  description?: React.ReactNode;
  displayPageLayout?: boolean;
  error?: FetchError;
  errorDetails?: string;
  location: Location;
  title?: string;
};

const UnauthorizedErrorPage = ({
  error = undefined,
  errorDetails = undefined,
  title = undefined,
  description = undefined,
  displayPageLayout = true,
  location: { pathname },
}: Props) => {
  const { t } = useTranslation('pages');
  const errorMessage = error ? (error.message ?? JSON.stringify(error)) : undefined;
  const pageDetails = error
    ? t('unauthorized.page_details', { pathname })
    : t('unauthorized.page_details_no_request', { pathname });
  const defaultDescription = (
    <>
      <p>{t('unauthorized.description_intro')}</p>
      <p>{t('unauthorized.description_hint')}</p>
    </>
  );
  const errorMessageString = createErrorMessageString(errorDetails, pageDetails, errorMessage);

  return (
    <ErrorPage
      title={title ?? t('unauthorized.title')}
      description={description ?? defaultDescription}
      displayPageLayout={displayPageLayout}>
      <dl>
        <dd>
          <pre className="content">
            <div className="pull-right">
              <ClipboardButton
                title={<Icon name="content_copy" />}
                bsSize="sm"
                text={errorMessageString}
                buttonTitle={t('errors.copy_error_details')}
              />
            </div>
            {errorDetails && <p>{errorDetails}</p>}
            <p>{pageDetails}</p>
            {errorMessage && <p>{errorMessage}</p>}
          </pre>
        </dd>
      </dl>
    </ErrorPage>
  );
};

export default withLocation(UnauthorizedErrorPage);
