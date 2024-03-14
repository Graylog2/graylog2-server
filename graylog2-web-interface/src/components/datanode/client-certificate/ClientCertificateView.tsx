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

import type { ClientCertCreateResponse } from 'components/datanode/hooks/useCreateDataNodeClientCert';
import copyToClipboard from 'util/copyToClipboard';
import { Button } from 'components/bootstrap';

type Props = {
  clientCerts: ClientCertCreateResponse
};

const Textarea = styled.textarea(({ theme }) => css`
  width: 100%;
  padding: 3px;
  resize: none;
  flex: 1;
  margin: 15px 0 7px;
  border: 1px solid ${theme.colors.variant.lighter.default};
  font-family: ${theme.fonts.family.monospace};
  font-size: ${theme.fonts.size.body};

  &:focus {
    border-color: ${theme.colors.variant.light.info};
    outline: none;
  }
`);

const ClientCertificateView = ({ clientCerts }: Props) => (
  <>
    <dt>Principal:</dt>
    <dd>{clientCerts.principal}</dd>
    <dt>Role:</dt>
    <dd>{clientCerts.role}</dd>
    <dt>CA certificate <Button bsStyle="info" bsSize="xs" onClick={() => copyToClipboard(clientCerts.ca_certificate)}>Copy to clipboard</Button></dt>
    <dd>
      <Textarea id="ca_certificate"
                value={clientCerts.ca_certificate}
                spellCheck={false} />
    </dd>
    <dt>Private key <Button bsStyle="info" bsSize="xs" onClick={() => copyToClipboard(clientCerts.private_key)}>Copy to clipboard</Button></dt>
    <dd>
      <Textarea id="private_key"
                value={clientCerts.private_key}
                spellCheck={false} />
    </dd>
    <dt>Certificate <Button bsStyle="info" bsSize="xs" onClick={() => copyToClipboard(clientCerts.certificate)}>Copy to clipboard</Button></dt>
    <dd>
      <Textarea id="certificate"
                value={clientCerts.certificate}
                spellCheck={false} />
    </dd>
  </>
);

export default ClientCertificateView;
