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
import React from 'react';
import styled, { css } from 'styled-components';
import { Trans, useTranslation } from 'react-i18next';

import { Panel } from 'components/bootstrap';
import useProductName from 'brand-customization/useProductName';

export const StyledPanel = styled(Panel)<{ bsStyle: string }>(
  ({ bsStyle = 'default', theme }) => css`
    &.panel {
      background-color: ${theme.colors.global.contentBackground};

      .panel-heading {
        color: ${theme.colors.variant.darker[bsStyle]};
      }
    }
    margin-top: ${theme.spacings.md} !important;
  `,
);

const JwtAuthenticationInfo = () => {
  const { t } = useTranslation('collectors', { keyPrefix: 'datanode' });
  const productName = useProductName();

  return (
    <StyledPanel bsStyle="info">
      <Panel.Heading>
        <Panel.Title componentClass="h3">{t('jwt_authentication.title')}</Panel.Title>
      </Panel.Heading>
      <Panel.Body>
        <p>{t('jwt_authentication.description', { productName })}</p>
        <p>
          <Trans t={t} i18nKey="jwt_authentication.add_snippet" components={{ codeSnippet: <code /> }} />
        </p>
        <pre>
          {/* eslint-disable-next-line graylog/no-untranslated-text -- configuration snippet */}
          {`jwt_auth_domain:
          description: "Authenticate via Json Web Token"
          http_enabled: true
          transport_enabled: true
          order: 1
          http_authenticator:
            type: jwt
            challenge: false
            config:
              signing_key: "base64 encoded HMAC key or public RSA/ECDSA pem key"
              jwt_header: "Authorization"
              jwt_url_parameter: null
              roles_key: "os_roles"
              subject_key: null
          authentication_backend:
            type: noop`}
        </pre>
        <p>
          <Trans t={t} i18nKey="jwt_authentication.replace_signing_key" components={{ codeSnippet: <code /> }} />
        </p>
        {/* eslint-disable-next-line graylog/no-untranslated-text -- shell command */}
        <pre>echo &quot;YOUR SECRET&quot; | base64</pre>
      </Panel.Body>
    </StyledPanel>
  );
};
export default JwtAuthenticationInfo;
