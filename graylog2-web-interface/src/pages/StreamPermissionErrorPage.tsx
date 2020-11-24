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
// @flow strict
import React from 'react';
import PropTypes from 'prop-types';

import FetchError from 'logic/errors/FetchError';

import UnauthorizedErrorPage from './UnauthorizedErrorPage';

type Props = {
  error: FetchError | undefined | null,
  missingStreamIds: string[],
};

const StreamPermissionErrorPage = ({ error, missingStreamIds = [] }: Props) => {
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
