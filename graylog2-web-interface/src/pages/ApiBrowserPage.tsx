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
import { useCallback, useRef, useEffect, useState } from 'react';
import styled, { css } from 'styled-components';

import { DocumentTitle } from 'components/common';
import { qualifyUrl } from 'util/URLUtils';

const StyledExplorerContainer = styled.div(
  ({ theme }) => css`
    margin-left: -25px;
    margin-right: -25px;
    margin-top: -11px;
    height: 89vh;
    width: 99vw;

    openapi-explorer {
      --bg: ${theme.colors.global.contentBackground};
      --bg2: ${theme.colors.global.background};
      --bg3: ${theme.colors.gray[90]};
      --fg: ${theme.colors.text.primary};
      --fg2: ${theme.colors.text.secondary};
      --fg3: ${theme.colors.text.disabled};
      --text-color: ${theme.colors.text.primary};
      --primary-color: ${theme.colors.variant.primary};
      --secondary-color: ${theme.colors.text.secondary};
      --border-color: ${theme.colors.gray[80]};
      --light-border-color: ${theme.colors.gray[90]};
      --header-bg: ${theme.colors.global.contentBackground};
      --header-fg: ${theme.colors.text.primary};
      --header-color-darker: ${theme.colors.gray[80]};
      --header-color-border: ${theme.colors.gray[70]};
      --nav-bg-color: ${theme.colors.global.navigationBackground};
      --nav-text-color: ${theme.colors.text.primary};
      --nav-hover-bg-color: ${theme.colors.gray[90]};
      --nav-hover-text-color: ${theme.colors.global.textAlt};
      --input-bg: ${theme.colors.input.background};
      --placeholder-color: ${theme.colors.input.placeholder};
      --selection-bg: ${theme.colors.brand.primary};
      --selection-fg: #fff;
      --overlay-bg: rgba(0, 0, 0, 0.4);
      --code-fg: ${theme.colors.text.primary};
      --code-border-color: ${theme.colors.gray[80]};
      --inline-code-fg: ${theme.colors.variant.darker?.danger ?? theme.colors.text.primary};
      --font-regular: ${theme.fonts.family.body};
      --font-mono: ${theme.fonts.family.monospace};
      --font-size-regular: ${theme.fonts.size.body};
      --font-size-small: ${theme.fonts.size.small};
      --font-size-mono: ${theme.fonts.size.small};
      --blue: ${theme.colors.variant.info};
      --green: ${theme.colors.variant.success};
      --red: ${theme.colors.variant.danger};
      --orange: ${theme.colors.variant.warning};
      --yellow: ${theme.colors.variant.warning};
    }
  `,
);

// noinspection JSUnusedGlobalSymbols
const ApiBrowserPage = () => {
  const explorerRef = useRef<HTMLElement>(null);
  const [loaded, setLoaded] = useState(false);

  useEffect(() => {
    import(/* webpackChunkName: "openapi-explorer" */ 'openapi-explorer').then(() => setLoaded(true));
  }, []);

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
  }, [handleRequest, loaded]);

  if (!loaded) {
    return (
      <DocumentTitle title="API Browser">
        <span>Loading...</span>
      </DocumentTitle>
    );
  }

  return (
    <DocumentTitle title="API Browser">
      <StyledExplorerContainer>
        {/* @ts-ignore - openapi-explorer is a web component */}
        <openapi-explorer
          ref={explorerRef}
          spec-url={qualifyUrl('/openapi.yaml')}
          server-url="api/"
          hide-authentication
          hide-server-selection
        >
          <div slot="overview-header" style={{ padding: '16px 16px 0', display: 'flex', alignItems: 'center', justifyContent: 'flex-end', gap: '8px' }}>
            <span>Download OpenAPI specification:</span>
            {/* eslint-disable-next-line jsx-a11y/control-has-associated-label */}
            <select onChange={(e) => {
              const path = e.target.value;

              if (path) {
                const link = document.createElement('a');
                link.href = qualifyUrl(path);
                link.download = `openapi${path.substring(path.lastIndexOf('.'))}`;
                link.click();
              }

              e.target.value = '';
            }}>
              <option value="">Select format</option>
              <option value="/openapi.json">JSON</option>
              <option value="/openapi.yaml">YAML</option>
            </select>
          </div>
        </openapi-explorer>
      </StyledExplorerContainer>
    </DocumentTitle>
  );
};

export default ApiBrowserPage;
