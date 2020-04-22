// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import { withRouter } from 'react-router';

import { FetchError } from 'logic/rest/FetchProvider';

import { Icon, ClipboardButton } from 'components/common';
import ErrorPage from 'components/errors/ErrorPage';

const createErrorMessageString = (errorDetails: ?string, pageDetails: string, errorMessage: string) => {
  const defaultText = `${pageDetails}\n${errorMessage}`;
  if (errorDetails) {
    return `${errorDetails}\n${defaultText}`;
  }
  return defaultText;
};

type Props = {
  description?: React.Node,
  error: FetchError,
  errorDetails?: string,
  location: {
    pathname: string,
  },
  title: string,
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
  error: PropTypes.shape({
    message: PropTypes.string.isRequired,
  }).isRequired,
  errorDetails: PropTypes.string,
  location: PropTypes.shape({
    pathname: PropTypes.string,
  }),
  title: PropTypes.string,
};

UnauthorizedErrorPage.defaultProps = {
  description: undefined,
  errorDetails: undefined,
  location: {},
  title: 'Missing Permissions',
};

export default withRouter(UnauthorizedErrorPage);
