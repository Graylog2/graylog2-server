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
import React, { useState } from 'react';
import styled from 'styled-components';

import Button from 'components/bootstrap/Button';
import Modal from 'components/bootstrap/Modal';
import Well from 'components/bootstrap/Well';
import Icon from 'components/common/Icon';
import DocumentTitle from 'components/common/DocumentTitle';
import { qualifyUrl } from 'util/URLUtils';
import LoginChrome from 'components/login/LoginChrome';
import type { ServerError } from 'stores/sessions/ServerAvailabilityStore';

const StyledIcon = styled(Icon)`
  margin-left: 6px;
`;

type Props = {
  server?: {
    up: false,
    error?: ServerError,
  }
};

const ServerUnavailablePage = ({ server }: Props) => {
  const [showDetails, setShowDetails] = useState(false);

  const _toggleDetails = () => setShowDetails(!showDetails);

  const _formatErrorMessage = () => {
    if (!showDetails) {
      return null;
    }

    const noInformationMessage = (
      <div>
        <hr />
        <p>There is no information available.</p>
      </div>
    );

    if (!server?.error) {
      return noInformationMessage;
    }

    const { error } = server;

    const errorDetails = [];

    if (error.message) {
      errorDetails.push(<dt key="error-title">Error message</dt>, <dd key="error-desc">{error.message}</dd>);
    }

    if (error.originalError) {
      const { originalError } = error;

      errorDetails.push(
        <dt key="status-original-request-title">Original Request</dt>,
        <dd key="status-original-request-content">{String(originalError.method)} {String(originalError.url)}</dd>,
      );

      errorDetails.push(
        <dt key="status-code-title">Status code</dt>,
        <dd key="status-code-desc">{String(originalError.status)}</dd>,
      );

      if (typeof originalError.toString === 'function') {
        errorDetails.push(
          <dt key="full-error-title">Full error message</dt>,
          <dd key="full-error-desc">{originalError.toString()}</dd>,
        );
      }
    }

    if (errorDetails.length === 0) {
      return noInformationMessage;
    }

    return (
      <div>
        <hr style={{ marginTop: 10, marginBottom: 10 }} />
        <p>This is the last response we received from the server:</p>
        <Well bsSize="small" style={{ whiteSpace: 'pre-line' }}>
          <dl style={{ marginBottom: 0 }}>
            {errorDetails}
          </dl>
        </Well>
      </div>
    );
  };

  const modalTitle = 'Server currently unavailable';

  return (
    <DocumentTitle title="Server unavailable">
      <LoginChrome>
        <Modal show onHide={() => {}}>
          <Modal.Header>
            <Modal.Title><Icon name="warning" /> {modalTitle}</Modal.Title>
          </Modal.Header>
          <Modal.Body>
            <div>
              <p>
                We are experiencing problems connecting to the Graylog server running on <i>{qualifyUrl('')}</i>.
                Please verify that the server is healthy and working correctly.
              </p>
              <p>You will be automatically redirected to the previous page once we can connect to the server.</p>
              <p>
                Do you need a hand?{' '}
                <a href="https://www.graylog.org/community-support" rel="noopener noreferrer" target="_blank">We can
                  help you
                </a>.
              </p>
              <div>
                <Button bsStyle="primary"
                        tabIndex={0}
                        onClick={_toggleDetails}
                        bsSize="sm">
                  {showDetails ? 'Less details' : 'More details'}
                  <StyledIcon name={showDetails ? 'keyboard_arrow_up' : 'keyboard_arrow_down'} />
                </Button>
                {_formatErrorMessage()}
              </div>
            </div>
          </Modal.Body>
        </Modal>
      </LoginChrome>
    </DocumentTitle>
  );
};

export default ServerUnavailablePage;
