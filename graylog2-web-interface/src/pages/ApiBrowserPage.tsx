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
import SwaggerUI from 'swagger-ui-react';
import 'swagger-ui-react/swagger-ui.css';

import { DocumentTitle } from 'components/common';
import { qualifyUrl } from 'util/URLUtils';

// noinspection JSUnusedGlobalSymbols
const ApiBrowserPage = () => (
  <DocumentTitle title="API Browser">
    <SwaggerUI
      url={qualifyUrl('/openapi.yaml')}
      filter
      deepLinking
      requestInterceptor={(req) => {
        req.headers['X-Requested-By'] = 'API Browser';
        return req;
      }}
      plugins={[
        () => ({
          // Hide authorization UI since the browser already has a valid session cookie which takes precedence over
          // any other auth mechanism even if users try to enable that through the Swagger UI.
          wrapComponents: {
            authorizeBtn: () => () => null,
            authorizeOperationBtn: () => () => null,
          },
        }),
      ]}
    />
  </DocumentTitle>
);

export default ApiBrowserPage;
