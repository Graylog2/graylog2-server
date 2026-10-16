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
import React, { useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import styled, { css } from 'styled-components';

import { Row, Col, ButtonToolbar, Button } from 'components/bootstrap';
import Spinner from 'components/common/Spinner';
import UserNotification from 'util/UserNotification';
import { DocumentTitle, PageHeader } from 'components/common';
import ContentPacksList from 'components/content-packs/ContentPacksList';
import ContentPackUploadControls from 'components/content-packs/ContentPackUploadControls';
import { deleteContentPack, installContentPack } from 'hooks/useContentPackMutations';
import useContentPacks from 'components/content-packs/hooks/useContentPacks';
import MarketplaceLink from 'components/support/MarketplaceLink';
import type { EntitySharePayload } from 'actions/permissions/EntityShareActions';
import CreateButton from 'components/common/CreateButton';

const ConfigurationBundles = styled.div(
  ({ theme }) => css`
    font-size: ${theme.fonts.size.body};
    font-weight: normal;
    margin-top: 15px;
  `,
);

const ContentPacksPage = () => {
  const { t } = useTranslation('content_packs');
  const { data, isInitialLoading, refetch } = useContentPacks();

  const _deleteContentPack = useCallback(
    (contentPackId: string) => {
      // eslint-disable-next-line no-alert
      if (window.confirm(t('delete_confirmation'))) {
        deleteContentPack(contentPackId).then(
          () => {
            UserNotification.success(t('delete_success'), 'Success');
            refetch();
          },
          (error) => {
            let err_message = error.message;
            const err_body = error.additional.body;

            if (err_body && err_body.message) {
              err_message = err_body.message;
            }

            UserNotification.error(t('delete_error_message', { error: err_message }), 'Error');
          },
        );
      }
    },
    [refetch, t],
  );

  const _installContentPack = useCallback(
    (contentPackId: string, contentPackRev: number, parameters: unknown, shareRequest: EntitySharePayload) => {
      installContentPack(contentPackId, contentPackRev, parameters, shareRequest).then(
        () => {
          UserNotification.success(t('install_success'), 'Success');
          refetch();
        },
        (error) => {
          UserNotification.error(t('install_error_message', { status: error, id: contentPackId }), 'Error');
        },
      );
    },
    [refetch, t],
  );

  if (isInitialLoading) {
    return <Spinner />;
  }

  const { content_packs: contentPacks, content_packs_metadata: contentPackMetadata } = data;

  return (
    <DocumentTitle title={t('title')}>
      <span>
        <PageHeader
          title={t('title')}
          topActions={<Button bsStyle="info">{t('button_text')}</Button>}
          actions={
            <ButtonToolbar>
              <ContentPackUploadControls />
              <CreateButton entityKey="Content Pack" />
            </ButtonToolbar>
          }>
          <span>
            {t('description')}
            <br />
            <MarketplaceLink prefix="Find more Content Packs in" />
          </span>
        </PageHeader>

        <Row className="content">
          <Col md={12}>
            <ConfigurationBundles>
              <ContentPacksList
                contentPacks={contentPacks}
                contentPackMetadata={contentPackMetadata}
                onDeletePack={_deleteContentPack}
                onInstall={_installContentPack}
              />
            </ConfigurationBundles>
          </Col>
        </Row>
      </span>
    </DocumentTitle>
  );
};

export default ContentPacksPage;
