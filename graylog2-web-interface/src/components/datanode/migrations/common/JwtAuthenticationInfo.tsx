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

import { Panel } from 'components/bootstrap';

export const StyledPanel = styled(Panel)<{ bsStyle: string }>(({ bsStyle = 'default', theme }) => css`
  &.panel {
    background-color: ${theme.colors.global.contentBackground};

    .panel-heading {
      color: ${theme.colors.variant.darker[bsStyle]};
    }
  }
  margin-top: ${theme.spacings.md} !important;
`);

const JwtAuthenticationInfo = () => (
  <StyledPanel bsStyle="info">
    <Panel.Heading>
      <Panel.Title componentClass="h3">JWT authentication</Panel.Title>
    </Panel.Heading>
    <Panel.Body>
      <p>
        Depending on how you secured your existing cluster, some preliminary changes are needed to the security configuration.
        We use JWT authentication to access OpenSearch from Graylog. In the next step, you have to manually enable JWT authentication
        in your existing OpenSearch cluster to make sure the data can be accessed in the data node.
      </p>
      <p>
        To do this, you should add the following snippet to your <code>opensearch-security/config.yml</code>
      </p>
      <pre>
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
        Please replace the signing key with your <code>GRAYLOG_PASSWORD_SECRET</code> in base64 encoding. To encode it, you can run
      </p>
      <pre>echo &quot;YOUR SECRET&quot; | base64</pre>
    </Panel.Body>
  </StyledPanel>
);
export default JwtAuthenticationInfo;
