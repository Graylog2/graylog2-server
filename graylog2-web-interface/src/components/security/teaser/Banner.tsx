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
import styled from 'styled-components';
import { useState } from 'react';

import Store from 'logic/local-storage/Store';
import { Alert, Modal, BootstrapModalWrapper } from 'components/bootstrap';
import { ExternalLinkButton } from 'components/common';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

const SECURITY_TEASER_MODAL_STORAGE_KEY = 'dismissed_security_teaser_modal';

const Container = styled.div`
  padding: 0 10px;
`;

const AlertInner = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
`;

const Header = styled.h2`
  font-weight: bold;
`;

const LeftCol = styled.div`
  display: flex;
  align-items: center;
  gap: 10px;
`;

const Banner = () => {
  const [showModal, setShowModal] = useState(() => Store.get(SECURITY_TEASER_MODAL_STORAGE_KEY) !== 'true');
  const sendTelemetry = useSendTelemetry();

  const dismissModal = () => {
    setShowModal(false);

    Store.set(SECURITY_TEASER_MODAL_STORAGE_KEY, 'true');

    sendTelemetry(TELEMETRY_EVENT_TYPE.SECURITY_APP.ASSET_CONFIG_PRIORITY_UPDATED, {
      app_pathname: 'security',
      app_section: 'teaser-page',
      app_action_value: 'dismiss-teaser-modal',
    });
  };

  return (
    <Container>
      <Alert bsStyle="info" displayIcon={false}>
        <AlertInner>
          <LeftCol>
            <Header>Security Demo</Header>
            For more information and booking a full demo of the product, visit the Graylog web site.
          </LeftCol>
          <ExternalLinkButton bsStyle="info" href="https://graylog.org/products/security">
            Graylog Web Site
          </ExternalLinkButton>
        </AlertInner>
      </Alert>
      {showModal && (
        <BootstrapModalWrapper showModal
                               onHide={dismissModal}
                               bsSize="lg">
          <Modal.Header closeButton>
            <Modal.Title>Welcome to Graylog Security!</Modal.Title>
          </Modal.Header>
          <Modal.Body>
            Graylog Security is designed to revolutionize cybersecurity for IT teams, offering the combined capabilities of SIEM,
            Security Analytics, Incident Investigation, and Anomaly Detection. By using our platform, you can work more efficiently,
            tackling critical tasks quicker, and mitigating risk caused by malicious actors and credential-based attacks.
          </Modal.Body>
        </BootstrapModalWrapper>
      )}
    </Container>
  );
};

export default Banner;
