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
import PropTypes from 'prop-types';

import FetchError from 'logic/errors/FetchError';
import { Icon, ClipboardButton } from 'components/common';
import ErrorPage from 'components/errors/ErrorPage';
import withLocation from 'routing/withLocation';
import type { Location } from 'routing/withLocation';

const createErrorMessageString = (errorDetails: string | null | undefined, pageDetails: string, errorMessage: string) => {
  const defaultText = `${pageDetails}\n${errorMessage}`;

  if (errorDetails) {
    return `${errorDetails}\n${defaultText}`;
  }

  return defaultText;
};

type Props = {
  description?: React.ReactNode,
  error: FetchError,
  errorDetails?: string,
  location: Location,
  title?: string,
};

const UnauthorizedErrorPage = ({ error, errorDetails, title, description, location: { pathname } }: Props) => {
  const errorMessage = error?.message || JSON.stringify(error);
  const pageDetails = `The permissions check for the following request failed,\nwhile trying to access ${pathname}.`;
  const defaultDescription = (
    <>
      <p>You do not have the required permissions to view this resource.</p>
      <p>Please contact your administrator and provide the error details.</p>
    </>
  );
  const errorMessageString = createErrorMessageString(errorDetails, pageDetails, errorMessage);

  return (
    <ErrorPage title={title} description={description ?? defaultDescription}>
      <dl>
        <dd>
          <pre className="content">
            <div className="pull-right">
              <ClipboardButton title={<Icon name="copy" fixedWidth />}
                               bsSize="sm"
                               text={errorMessageString}
                               buttonTitle="Copy error details to clipboard" />
            </div>
            {errorDetails && (
              <p>
                {errorDetails}
              </p>
            )}
            <p>
              {pageDetails}
            </p>
            <p>
              {errorMessage}
            </p>
          </pre>
        </dd>
      </dl>
    </ErrorPage>
  );
};

UnauthorizedErrorPage.propTypes = {
  description: PropTypes.node,
  errorDetails: PropTypes.string,
  title: PropTypes.string,
};

UnauthorizedErrorPage.defaultProps = {
  description: undefined,
  errorDetails: undefined,
  title: 'Missing Permissions',
};

export default withLocation(UnauthorizedErrorPage);
