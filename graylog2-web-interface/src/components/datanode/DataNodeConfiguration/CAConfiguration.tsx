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
import styled from 'styled-components';

import { Tabs, Alert } from 'components/bootstrap';
import CACreateForm from 'components/datanode/DataNodeConfiguration/CACreateForm';
import CAUpload from 'components/datanode/DataNodeConfiguration/CAUpload';
import DocumentationLink from 'components/support/DocumentationLink';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import useProductName from 'brand-customization/useProductName';

const StyledAlert = styled(Alert)`
  margin-top: 10px;
  margin-bottom: 10px;
`;

const TAB_KEYS = ['create', 'upload'];

const UploadCA = 'Upload CA';

const CAConfiguration = () => {
  const sendTelemetry = useSendTelemetry();
  const productName = useProductName();

  const handleTabSwitch = (value: string | null) => {
    sendTelemetry(
      value === TAB_KEYS[1]
        ? TELEMETRY_EVENT_TYPE.DATANODE_MIGRATION.CA_UPLOAD_TAB_CLICKED
        : TELEMETRY_EVENT_TYPE.DATANODE_MIGRATION.CA_CREATE_TAB_CLICKED,
      {
        app_pathname: 'datanode',
        app_section: 'migration',
      },
    );
  };

  return (
    <>
      <h2>Configure Certificate Authority</h2>
      <p>
        In this step you can either upload or create a new certificate authority.
        <br />
        The certificate authority will provision and manage certificates for your Data Nodes more easily.
      </p>
      <StyledAlert bsStyle="info" title="Reusing certificates">
        If your existing cluster uses certificates, by default these will get replaced with the {productName} CA and
        automatically generated certificates during provisioning of the data nodes in the next step. If you want to
        include your own CA, you can upload an existing certificate. Please see{' '}
        <DocumentationLink page="graylog-data-node" text={`${productName} Data Node - Getting Started`} /> for more
        information.
      </StyledAlert>
      <Tabs defaultValue={TAB_KEYS[0]} onChange={handleTabSwitch}>
        <Tabs.List>
          <Tabs.Tab value={TAB_KEYS[0]}>Create new CA</Tabs.Tab>
          <Tabs.Tab value={TAB_KEYS[1]}>{UploadCA}</Tabs.Tab>
        </Tabs.List>
        <Tabs.Panel value={TAB_KEYS[0]}>
          <CACreateForm />
        </Tabs.Panel>
        <Tabs.Panel value={TAB_KEYS[1]}>
          <CAUpload />
        </Tabs.Panel>
      </Tabs>
    </>
  );
};

export default CAConfiguration;
