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
import { Trans, useTranslation } from 'react-i18next';
import styled from 'styled-components';

import Button from 'components/bootstrap/Button';
import Modal from 'components/bootstrap/Modal';
import Well from 'components/bootstrap/Well';
import Icon from 'components/common/Icon';
import DocumentTitle from 'components/common/DocumentTitle';
import { qualifyUrl } from 'util/URLUtils';
import LoginChrome from 'components/login/LoginChrome';
import useProductName from 'brand-customization/useProductName';
import type { ServerError } from 'contexts/ServerAvailabilityContext';
import DocsHelper from 'util/DocsHelper';

const StyledIcon = styled(Icon)`
  margin-left: 6px;
`;

type Props = {
  server?: {
    up: false;
    error?: ServerError;
  };
};

const HelpLink = ({ children = undefined }: React.PropsWithChildren<{}>) => (
  <a href={DocsHelper.PAGES.SERVER_UNAVAILABLE} rel="noopener noreferrer" target="_blank">
    {children}
  </a>
);

const ServerUnavailablePage = ({ server = undefined }: Props) => {
  const { t } = useTranslation('pages');
  const productName = useProductName();
  const [showDetails, setShowDetails] = useState(false);

  const _toggleDetails = () => setShowDetails(!showDetails);

  const _formatErrorMessage = () => {
    if (!showDetails) {
      return null;
    }

    const noInformationMessage = (
      <div>
        <hr />
        <p>{t('server_unavailable.no_information')}</p>
      </div>
    );

    if (!server?.error) {
      return noInformationMessage;
    }

    const { error } = server;

    const errorDetails = [];

    if (error.message) {
      errorDetails.push(
        <dt key="error-title">{t('server_unavailable.error_message')}</dt>,
        <dd key="error-desc">{error.message}</dd>,
      );
    }

    if (error.originalError) {
      const { originalError } = error;

      errorDetails.push(
        <dt key="status-original-request-title">{t('server_unavailable.original_request')}</dt>,
        <dd key="status-original-request-content">
          {String(originalError.method)} {String(originalError.url)}
        </dd>,
      );

      errorDetails.push(
        <dt key="status-code-title">{t('server_unavailable.status_code')}</dt>,
        <dd key="status-code-desc">{String(originalError.status)}</dd>,
      );

      if (typeof originalError.toString === 'function') {
        errorDetails.push(
          <dt key="full-error-title">{t('server_unavailable.full_error_message')}</dt>,
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
        <p>{t('server_unavailable.last_response')}</p>
        <Well bsSize="small" style={{ whiteSpace: 'pre-line' }}>
          <dl style={{ marginBottom: 0 }}>{errorDetails}</dl>
        </Well>
      </div>
    );
  };

  const modalTitle = t('server_unavailable.modal_title');

  return (
    <DocumentTitle title={t('server_unavailable.document_title')}>
      <LoginChrome>
        <Modal show onHide={() => {}}>
          <Modal.Header>
            <Modal.Title>
              <Icon name="warning" /> {modalTitle}
            </Modal.Title>
          </Modal.Header>
          <Modal.Body>
            <div>
              <p>
                <Trans
                  t={t}
                  i18nKey="server_unavailable.intro"
                  values={{ productName, address: qualifyUrl('') }}
                  components={{ serverAddress: <i /> }}
                />
              </p>
              <p>{t('server_unavailable.redirect_hint')}</p>
              <p>
                <Trans t={t} i18nKey="server_unavailable.need_help" components={{ helpLink: <HelpLink /> }} />
              </p>
              <div>
                <Button bsStyle="primary" tabIndex={0} onClick={_toggleDetails} bsSize="sm">
                  {showDetails ? t('server_unavailable.less_details') : t('server_unavailable.more_details')}
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
