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
import { useCallback, useRef, useEffect } from 'react';
import 'openapi-explorer';

import { DocumentTitle } from 'components/common';
import { qualifyUrl } from 'util/URLUtils';

// noinspection JSUnusedGlobalSymbols
const ApiBrowserPage = () => {
  const explorerRef = useRef<HTMLElement>(null);

  const handleRequest = useCallback((event: CustomEvent) => {
    event.detail.request.headers.append('X-Requested-By', 'API Browser');
  }, []);

  useEffect(() => {
    const el = explorerRef.current;

    if (el) {
      el.addEventListener('request', handleRequest as EventListener);
    }

    return () => {
      if (el) {
        el.removeEventListener('request', handleRequest as EventListener);
      }
    };
  }, [handleRequest]);

  return (
    <DocumentTitle title="API Browser">
      {/* @ts-ignore - openapi-explorer is a web component */}
      <openapi-explorer
        ref={explorerRef}
        spec-url={qualifyUrl('/openapi.yaml')}
        server-url={qualifyUrl('/')}
        hide-authentication
        hide-server-selection
      />
    </DocumentTitle>
  );
};

export default ApiBrowserPage;
