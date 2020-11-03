// @flow strict
import React from 'react';
import PropTypes from 'prop-types';

import { FetchError } from 'logic/rest/FetchProvider';

import UnauthorizedErrorPage from './UnauthorizedErrorPage';

type Props = {
  error: FetchError,
  missingStreamIds: string[],
};

const StreamPermissionErrorPage = ({ error = {}, missingStreamIds = [] }: Props) => {
  const description = (
    <>
      <p>This resource includes streams you do not have permissions for.</p>
      <p>Please contact your administrator and provide the error details which include a list of streams you need access to.</p>
    </>
  );
  const streamIds = missingStreamIds.length > 0
    ? missingStreamIds
    : error?.additional?.body?.streams;
  const errorDetails = streamIds?.length > 0 ? `You need permissions for streams with the id: ${streamIds.join(', ')}.` : undefined;

  return (
    <UnauthorizedErrorPage error={error} description={description} title="Missing Stream Permissions" errorDetails={errorDetails} />
  );
};

StreamPermissionErrorPage.propTypes = {
  error: PropTypes.shape({
    message: PropTypes.string.isRequired,
    additional: PropTypes.shape({
      body: PropTypes.shape({
        streams: PropTypes.arrayOf(PropTypes.string),
      }),
    }),
  }).isRequired,
  missingStreamIds: PropTypes.arrayOf(PropTypes.string),
};

StreamPermissionErrorPage.defaultProps = {
  missingStreamIds: [],
};

export default StreamPermissionErrorPage;
