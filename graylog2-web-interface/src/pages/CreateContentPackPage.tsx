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
import { useTranslation } from 'react-i18next';

import { SystemCatalog } from '@graylog/server-api';

import { LinkContainer, DocumentTitle, PageHeader } from 'components/common';
import Routes from 'routing/Routes';
import { Button } from 'components/bootstrap';
import UserNotification from 'util/UserNotification';
import ContentPackEdit from 'components/content-packs/ContentPackEdit';
import ContentPack from 'logic/content-packs/ContentPack';
import Entity from 'logic/content-packs/Entity';
import EntityIndex from 'logic/content-packs/EntityIndex';
import { createContentPack } from 'hooks/useContentPackMutations';
import useHistory from 'routing/useHistory';
import useProductName from 'brand-customization/useProductName';
import MarketplaceLink from 'components/support/MarketplaceLink';
import useEntityIndex from 'components/content-packs/hooks/useEntityIndex';

const CreateContentPackPage = () => {
  const { t } = useTranslation('content_packs');
  const productName = useProductName();
  const history = useHistory();
  const { entityIndex } = useEntityIndex();
  const [contentPackState, setContentPackState] = useState({
    contentPack: ContentPack.builder().build(),
    appliedParameter: {},
    selectedEntities: {},
    fetchedEntities: undefined,
  });

  const _onStateChanged = (newState: {
    contentPack: ContentPack;
    selectedEntities: unknown;
    appliedParameter: unknown;
  }) => {
    setContentPackState((cur) => ({
      ...cur,
      contentPack: newState.contentPack || cur.contentPack,
      selectedEntities: newState.selectedEntities || cur.selectedEntities,
      appliedParameter: newState.appliedParameter || cur.appliedParameter,
    }));
  };

  const _onSave = () => {
    const { contentPack } = contentPackState;

    createContentPack(contentPack.toJSON()).then(
      () => {
        UserNotification.success(t('import_success_message'), t('import_success_title'));
        history.push(Routes.SYSTEM.CONTENTPACKS.LIST);
      },
      (response) => {
        const message = t('import_error_message', { productName });
        const title = t('import_error_title');
        let smallMessage = '';

        if (response.additional && response.additional.body && response.additional.body.message) {
          smallMessage = `<br /><small>${response.additional.body.message}</small>`;
        }

        UserNotification.error(message + smallMessage, title);
      },
    );
  };

  const _getEntities = (selectedEntities) => {
    const { contentPack } = contentPackState;
    const payload = Object.keys(selectedEntities)
      .reduce((acc, entityType) => acc.concat(selectedEntities[entityType]), [])
      .filter((e) => e instanceof EntityIndex)
      .map((entity) => entity.toJSON());

    SystemCatalog.resolveEntities({ entities: payload as any }).then((result: any) => {
      const newContentPack = contentPack
        .toBuilder()
        /* Mark entities from server */
        .entities(result.entities.map((e: any) => Entity.fromJSON(e, true, contentPack.parameters)))
        .build();
      const fetchedEntities = result.entities.map((e: any) => Entity.fromJSON(e, false, contentPack.parameters));

      setContentPackState((cur) => ({ ...cur, contentPack: newContentPack, fetchedEntities }));
    });
  };

  return (
    <DocumentTitle title={t('create_page_title')}>
      <span>
        <PageHeader
          title={t('create_header_title')}
          topActions={
            <LinkContainer to={Routes.SYSTEM.CONTENTPACKS.LIST}>
              <Button bsStyle="info">{t('create_button_text')}</Button>
            </LinkContainer>
          }>
          <span>
            {t('create_description')}
            <MarketplaceLink prefix={t('marketplace_prefix')} />
          </span>
        </PageHeader>
        <ContentPackEdit
          contentPack={contentPackState.contentPack}
          onGetEntities={_getEntities}
          onStateChange={_onStateChanged}
          fetchedEntities={contentPackState.fetchedEntities}
          selectedEntities={contentPackState.selectedEntities}
          appliedParameter={contentPackState.appliedParameter}
          entityIndex={entityIndex}
          onSave={_onSave}
        />
      </span>
    </DocumentTitle>
  );
};

export default CreateContentPackPage;
