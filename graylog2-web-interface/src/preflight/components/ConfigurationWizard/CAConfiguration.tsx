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
import { useTranslation } from 'react-i18next';

import Tabs from 'components/bootstrap/Tabs';
import { Title, Space } from 'preflight/components/common';

import CACreateForm from './CACreateForm';
import CAUpload from './CAUpload';

const StyledTabs = styled(Tabs)(
  ({ theme }) => css`
    button {
      font-size: ${theme.fonts.size.body};
    }
  `,
);

const CAConfiguration = () => {
  const { t } = useTranslation('preflight');

  return (
    <>
      <Title order={3}>{t('ca_configuration.title')}</Title>
      <p>
        {t('ca_configuration.explanation_upload_or_create')}
        <br />
        {t('ca_configuration.explanation_provisioning')}
      </p>
      <Space h="md" />
      <StyledTabs defaultValue="create">
        <Tabs.List>
          <Tabs.Tab value="create">{t('ca_configuration.create_tab')}</Tabs.Tab>
          <Tabs.Tab value="upload">{t('ca_configuration.upload_tab')}</Tabs.Tab>
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
