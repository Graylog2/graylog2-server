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
import { Trans, useTranslation } from 'react-i18next';

import { Panel } from 'components/bootstrap';

export const StyledPanel = styled(Panel)<{ bsStyle: string }>(
  ({ bsStyle = 'default', theme }) => css`
    &.panel {
      background-color: ${theme.colors.global.contentBackground};

      .panel-heading {
        color: ${theme.colors.variant.darker[bsStyle]};
      }
    }
    margin-top: ${theme.spacings.md} !important;
  `,
);

const InPlaceMigrationInfo = () => {
  const { t } = useTranslation('collectors', { keyPrefix: 'datanode' });

  return (
    <StyledPanel bsStyle="info">
      <Panel.Heading>
        <Panel.Title componentClass="h3">{t('in_place_migration.title')}</Panel.Title>
      </Panel.Heading>
      <Panel.Body>
        <Trans t={t} i18nKey="in_place_migration_info.configuration" components={{ codeSnippet: <code /> }} />
        <br />
        {t('in_place_migration_info.permissions')}
        <br />
        {/* eslint-disable-next-line graylog/no-untranslated-text -- shell command */}
        <code>sudo chmod -R 757 &lt;your_data_directory&gt;</code>
      </Panel.Body>
    </StyledPanel>
  );
};
export default InPlaceMigrationInfo;
