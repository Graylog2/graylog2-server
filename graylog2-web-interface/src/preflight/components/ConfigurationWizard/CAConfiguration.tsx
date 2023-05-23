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
import styled, { css } from 'styled-components';

import { Title, Space, Tabs, Alert } from 'preflight/components/common';

import CACreateForm from './CACreateForm';
import CAUpload from './CAUpload';

const StyledTabs = styled(Tabs)(({ theme }) => css`
  button {
    font-size: ${theme.fonts.size.body.rem};
  }
`);

const _isSecureConnection = () => {
  if (window.location.protocol !== 'https:') {
    return false;
  }

  return window.isSecureContext;
};

const CAConfiguration = () => {
  const isSecureConnection = _isSecureConnection();

  return (
    <>
      <Title order={3}>Configure Certificate Authority</Title>
      <p>
        In this first step you can either upload or create a new certificate authority.<br />
        Using it we can provision your data nodes with certificates easily.
      </p>
      {!isSecureConnection ? (
        <Alert type="warning">
          Your connection is not secure. Please be aware the information will be send to the server unencrypted.
          This includes for example the CA you upload.
        </Alert>
      ) : <Space h="md" />}
      <StyledTabs defaultValue="upload">
        <Tabs.List>
          <Tabs.Tab value="upload">Upload CA</Tabs.Tab>
          <Tabs.Tab value="create">Create new CA</Tabs.Tab>
        </Tabs.List>

        <Tabs.Panel value="upload" pt="xs">
          <CAUpload />
        </Tabs.Panel>
        <Tabs.Panel value="create" pt="xs">
          <CACreateForm />
        </Tabs.Panel>
      </StyledTabs>
    </>
  );
};

export default CAConfiguration;
