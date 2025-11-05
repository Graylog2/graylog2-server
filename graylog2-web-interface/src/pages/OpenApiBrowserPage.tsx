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
import { useEffect, useRef } from 'react';

import { DocumentTitle } from 'components/common';

// Using Redoc via CDN to avoid webpack bundling issues (POC approach)
// See: https://redocly.com/docs/redoc/deployment/html
const OpenApiBrowserPage = () => {
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    // Load Redoc script from CDN
    const script = document.createElement('script');
    script.src = 'https://cdn.redoc.ly/redoc/latest/bundles/redoc.standalone.js';
    script.async = true;
    document.body.appendChild(script);

    script.onload = () => {
      // Initialize Redoc once script is loaded
      // @ts-ignore - Redoc loaded from CDN
      if (containerRef.current && window.Redoc) {
        // @ts-ignore
        window.Redoc.init(
          '/api/openapi/yaml',
          {
            nativeScrollbars: true,
            theme: {
              colors: {
                primary: {
                  main: '#dd4400',
                },
              },
            },
          },
          containerRef.current,
        );
      }
    };

    return () => {
      // Cleanup script on unmount
      document.body.removeChild(script);
    };
  }, []);

  return (
    <DocumentTitle title="OpenAPI Browser">
      <div ref={containerRef} />
    </DocumentTitle>
  );
};

export default OpenApiBrowserPage;
