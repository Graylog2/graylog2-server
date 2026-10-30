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
  title = 'Missing Permissions',
  description = undefined,
  displayPageLayout = true,
  location: { pathname },
}: Props) => {
  const errorMessage = error ? (error.message ?? JSON.stringify(error)) : undefined;
  const pageDetails = error
    ? `The permissions check for the following request failed,\nwhile trying to access ${pathname}.`
    : `The permissions check failed while trying to access ${pathname}.`;
  const defaultDescription = (
    <>
      <p>You do not have the required permissions to view this resource.</p>
      <p>Please contact your administrator and provide the error details.</p>
    </>
  );
  const errorMessageString = createErrorMessageString(errorDetails, pageDetails, errorMessage);

  return (
    <ErrorPage title={title} description={description ?? defaultDescription} displayPageLayout={displayPageLayout}>
      <dl>
        <dd>
          <pre className="content">
            <div className="pull-right">
              <ClipboardButton
                title={<Icon name="content_copy" />}
                bsSize="sm"
                text={errorMessageString}
                buttonTitle="Copy error details to clipboard"
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
