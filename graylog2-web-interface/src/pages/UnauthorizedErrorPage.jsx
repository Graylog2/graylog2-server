// @flow strict
import React from 'react';
import PropTypes from 'prop-types';
import { withRouter } from 'react-router';

import { FetchError } from 'logic/rest/FetchProvider';

import { Icon, ClipboardButton } from 'components/common';
import ErrorPage from 'components/errors/ErrorPage';

type Props = {
  error: FetchError,
  location: {
    pathname: string
  }
}

const UnauthorizedErrorPage = ({ error, location: { pathname } }: Props) => {
  const errorMessage = error?.message || JSON.stringify(error);
  const pageDetails = `The permissions check for the following request failed,\nwhile tying to access ${pathname}.`;
  const description = (
    <>
      <p>You do not have the required permissions to view this ressource.</p>
      <p>Please contact your administrator and provide the error details.</p>
    </>
  );
  return (
    <ErrorPage title="Missing Permissions" description={description}>
      <dl>
        <dd>
          <pre className="content">
            <div className="pull-right">
              <ClipboardButton title={<Icon name="copy" fixedWidth />}
                               bsSize="sm"
                               text={`${pageDetails}\n${errorMessage}`}
                               buttonTitle="Copy error details to clipboard" />
            </div>
            {pageDetails}
            <br />
            <br />
            {errorMessage}
          </pre>
        </dd>
      </dl>
    </ErrorPage>
  );
};

UnauthorizedErrorPage.propTypes = {
  error: PropTypes.shape({
    message: PropTypes.string.isRequired,
  }).isRequired,
  location: PropTypes.shape({
    pathname: PropTypes.string,
  }),
};

UnauthorizedErrorPage.defaultProps = {
  location: {},
};

export default withRouter(UnauthorizedErrorPage);
