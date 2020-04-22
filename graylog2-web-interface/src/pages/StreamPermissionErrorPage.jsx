// @flow strict
import React from 'react';
import PropTypes from 'prop-types';
import { withRouter } from 'react-router';

import { FetchError } from 'logic/rest/FetchProvider';

import UnauthorizedErrorPage from './UnauthorizedErrorPage';

type Props = {
  error: FetchError,
};

const StreamPermissionErrorPage = ({ error }: Props) => {
  const description = (
    <>
      <p>This resource includes streams you do not have permissions for.</p>
      <p>Please contact your administrator and provide the error details which include a list of streams you need access to.</p>
    </>
  );
  const streamIds = error?.additional.body?.streams;
  const errorDetails = streamIds && streamIds.length > 0 && `You need permissions for streams with the id: ${streamIds.join(', ')}.`;
  return (
    <UnauthorizedErrorPage error={error} description={description} title="Missing Stream Permissions" errorDetails={errorDetails} />
  );
};

StreamPermissionErrorPage.propTypes = {
  error: PropTypes.shape({
    message: PropTypes.string.isRequired,
  }).isRequired,
};

export default withRouter(StreamPermissionErrorPage);
