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
import { useState } from 'react';
import styled, { css } from 'styled-components';

import { Alert, Button, ButtonToolbar } from 'components/bootstrap';
import { RelativeTime, Spinner } from 'components/common';
import useCurrentUser from 'hooks/useCurrentUser';
import useProductName from 'brand-customization/useProductName';
import useLocation from 'routing/useLocation';

import parseAuthorizationParams from './parseAuthorizationParams';
import redirectToClient from './redirectToClient';
import useMcpAuthorizationDecision from './useMcpAuthorizationDecision';
import useMcpAuthorizationRequest from './useMcpAuthorizationRequest';

const Container = styled.div(
  ({ theme }) => css`
    display: flex;
    flex-direction: column;
    gap: ${theme.spacings.md};
    text-align: left;
  `,
);

const Title = styled.h2(
  ({ theme }) => css`
    font-size: ${theme.fonts.size.h3};
  `,
);

const Details = styled.dl(
  ({ theme }) => css`
    display: grid;
    grid-template-columns: max-content auto;
    gap: ${theme.spacings.xs} ${theme.spacings.md};
    margin: 0;

    dd {
      margin: 0;
      word-break: break-all;
    }
  `,
);

const McpOAuthConsent = () => {
  const { search } = useLocation();
  const params = parseAuthorizationParams(search);
  const currentUser = useCurrentUser();
  const productName = useProductName();
  const { data: request, isLoading, error } = useMcpAuthorizationRequest(params);
  const { decide, isPending, error: decisionError } = useMcpAuthorizationDecision(params);
  const [decided, setDecided] = useState<boolean | undefined>();

  const onDecision = (approved: boolean) =>
    decide(approved).then((redirectUri) => {
      setDecided(approved);
      redirectToClient(redirectUri);
    });

  if (isLoading) {
    return <Spinner text="Loading authorization request..." delay={0} />;
  }

  if (error) {
    return (
      <Alert bsStyle="danger" title="Invalid authorization request">
        {error.responseMessage ?? 'The authorization request could not be validated.'} Please start connecting your MCP
        client again.
      </Alert>
    );
  }

  if (decided !== undefined) {
    return (
      <Alert bsStyle={decided ? 'success' : 'info'} title={decided ? 'Access granted' : 'Access denied'}>
        You can close this window and return to {request.clientName}.
      </Alert>
    );
  }

  return (
    <Container>
      <Title>Authorize {request.clientName}</Title>
      <p>
        <strong>{request.clientName}</strong> wants to connect to the {productName} MCP server as{' '}
        <strong>{currentUser.username}</strong>. It will be able to use all MCP tools with your permissions.
      </p>
      <Details>
        <dt>Redirects to</dt>
        <dd>{request.redirectHost}</dd>
        <dt>Registered</dt>
        <dd>
          <RelativeTime dateTime={request.clientRegisteredAt} />
        </dd>
      </Details>
      <Alert bsStyle="warning">
        Only allow access if you just started connecting this client to {productName}. Client names are chosen by the
        client and are not verified.
      </Alert>
      {decisionError && (
        <Alert bsStyle="danger">{decisionError.responseMessage ?? 'Saving your decision failed.'}</Alert>
      )}
      <ButtonToolbar>
        <Button onClick={() => onDecision(false)} disabled={isPending}>
          Deny
        </Button>
        <Button bsStyle="success" onClick={() => onDecision(true)} disabled={isPending}>
          Allow access
        </Button>
      </ButtonToolbar>
    </Container>
  );
};

export default McpOAuthConsent;
