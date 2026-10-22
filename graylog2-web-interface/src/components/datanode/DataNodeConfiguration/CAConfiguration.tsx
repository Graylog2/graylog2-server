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
import { Trans, useTranslation } from 'react-i18next';

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

const GettingStartedLink = ({ children = undefined }: React.PropsWithChildren) => (
  <DocumentationLink page="graylog-data-node" text={children} />
);

const CAConfiguration = () => {
  const { t } = useTranslation('collectors', { keyPrefix: 'datanode' });
  const sendTelemetry = useSendTelemetry('migration');
  const productName = useProductName();

  const handleTabSwitch = (value: string | null) => {
    sendTelemetry(
      value === TAB_KEYS[1]
        ? TELEMETRY_EVENT_TYPE.DATANODE_MIGRATION.CA_UPLOAD_TAB_CLICKED
        : TELEMETRY_EVENT_TYPE.DATANODE_MIGRATION.CA_CREATE_TAB_CLICKED,
      {
        app_pathname: 'datanode',
      },
    );
  };

  return (
    <>
      <h2>{t('ca_configuration.title')}</h2>
      <p>
        {t('ca_configuration.description_upload_or_create')}
        <br />
        {t('ca_configuration.description_provision')}
      </p>
      <StyledAlert bsStyle="info" title={t('ca_configuration.reusing_certificates_title')}>
        <Trans
          t={t}
          i18nKey="ca_configuration.reusing_certificates"
          values={{ productName }}
          components={{ docsLink: <GettingStartedLink /> }}
        />
      </StyledAlert>
      <Tabs defaultValue={TAB_KEYS[0]} onChange={handleTabSwitch}>
        <Tabs.List>
          <Tabs.Tab value={TAB_KEYS[0]}>{t('ca_configuration.create_tab')}</Tabs.Tab>
          <Tabs.Tab value={TAB_KEYS[1]}>{t('ca_configuration.upload_tab')}</Tabs.Tab>
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
