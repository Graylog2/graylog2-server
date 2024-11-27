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
import { useEffect, useMemo, useState } from 'react';
import cloneDeep from 'lodash/cloneDeep';
import groupBy from 'lodash/groupBy';

import { LinkContainer } from 'components/common/router';
import Routes from 'routing/Routes';
import { Button } from 'components/bootstrap';
import UserNotification from 'util/UserNotification';
import { DocumentTitle, PageHeader } from 'components/common';
import ValueReferenceData from 'util/ValueReferenceData';
import ContentPackEdit from 'components/content-packs/ContentPackEdit';
import Entity from 'logic/content-packs/Entity';
import { CatalogActions, CatalogStore } from 'stores/content-packs/CatalogStore';
import { ContentPacksActions, ContentPacksStore } from 'stores/content-packs/ContentPacksStore';
import useParams from 'routing/useParams';
import useHistory from 'routing/useHistory';
import { useStore } from 'stores/connect';

const EditContentPackPage = () => {
  const { entityIndex } = useStore(CatalogStore);
  const {} = useStore(ContentPacksStore);
  const { contentPackId, contentPackRev } = useParams<{ contentPackId: string, contentPackRev: string }>();
  const history = useHistory();
  const [selectedEntities, setSelectedEntities] = useState({});
  const [appliedParameter, setAppliedParameter] = useState({});
  const [contentPack, setContentPack] = useState(undefined);
  const [contentPackEntities, setContentPackEntities] = useState(undefined);
  const [fetchedEntities, setFetchedEntities] = useState(undefined);

  useEffect(() => {
    ContentPacksActions.get(contentPackId).then((result) => {
      const { contentPackRevisions } = result;
      const originContentPackRev = contentPackRev;
      const newContentPack = contentPackRevisions.createNewVersionFromRev(originContentPackRev);

      setContentPack(newContentPack);
      setContentPackEntities(cloneDeep(newContentPack.entities));

      CatalogActions.showEntityIndex();
    });
  }, []);

  const entityCatalog = useMemo(() => {
    if (!contentPack || !entityIndex) {
      return {};
    }

    const groupedContentPackEntities = groupBy(contentPackEntities, 'type.name');
    const newEntityCatalog = Object.keys(entityIndex)
      .reduce((result, entityType) => {
        /* eslint-disable-next-line no-param-reassign */
        result[entityType] = entityIndex[entityType].concat(groupedContentPackEntities[entityType] || []);

        return result;
      }, {});

    return newEntityCatalog;
  }, [contentPack, entityIndex, contentPackEntities]);

  useEffect(() => {
    if (!contentPack || !entityIndex) {
      return;
    }

    const newSelectedEntities = contentPack.entities.reduce((result, entity) => {
      if (entityCatalog[entity.type.name]
        && entityCatalog[entity.type.name].findIndex((fetchedEntity) => fetchedEntity.id === entity.id) >= 0) {
        const newResult = result;

        newResult[entity.type.name] = result[entity.type.name] || [];
        newResult[entity.type.name].push(entity);

        return newResult;
      }

      return result;
    }, {});

    setSelectedEntities(newSelectedEntities);
  }, [contentPack, entityIndex]);

  useEffect(() => {
    if (!contentPack) {
      return;
    }

    const newAppliedParameter = contentPack.entities.reduce((result, entity) => {
      const entityData = new ValueReferenceData(entity.data);
      const configPaths = entityData.getPaths();

      const paramMap = Object.keys(configPaths).filter((path) => configPaths[path].isValueParameter()).map((path) => ({ configKey: path, paramName: configPaths[path].getValue(), readOnly: true }));
      const newResult = result;

      if (paramMap.length > 0) {
        newResult[entity.id] = paramMap;
      }

      return newResult;
    }, {});

    setAppliedParameter(newAppliedParameter);
  }, [contentPack]);

  const _onStateChanged = (newState) => {
    setContentPack(newState.contentPack || contentPack);
    setSelectedEntities(newState.selectedEntities || selectedEntities);
    setAppliedParameter(newState.appliedParameter || appliedParameter);
  };

  const _onSave = () => {
    ContentPacksActions.create(contentPack.toJSON())
      .then(
        () => {
          UserNotification.success('Content pack imported successfully', 'Success!');
          history.push(Routes.SYSTEM.CONTENTPACKS.LIST);
        },
        (response) => {
          const message = 'Error importing content pack, please ensure it is a valid JSON file. Check your '
            + 'Graylog logs for more information.';
          const title = 'Could not import content pack';
          let smallMessage = '';

          if (response.additional && response.additional.body && response.additional.body.message) {
            smallMessage = `<br /><small>${response.additional.body.message}</small>`;
          }

          UserNotification.error(message + smallMessage, title);
        },
      );
  };

  const _getEntities = (selectedEntities) => {
    CatalogActions.getSelectedEntities(selectedEntities).then((result) => {
      const contentPackEntities = Object.keys(selectedEntities)
        .reduce((acc, entityType) => acc.concat(selectedEntities[entityType]), []).filter((e) => e instanceof Entity);
      /* Mark entities from server */
      const entities = contentPackEntities.concat(result.entities.map((e) => Entity.fromJSON(e, true)));
      const builtContentPack = contentPack.toBuilder()
        .entities(entities)
        .build();

      setContentPack(builtContentPack);
      setFetchedEntities(builtContentPack.entities);
    });
  };

  return (
    <DocumentTitle title="Content packs">
      <span>
        <PageHeader title="Edit content pack"
                    topActions={(
                      <LinkContainer to={Routes.SYSTEM.CONTENTPACKS.LIST}>
                        <Button bsStyle="info">Content Packs</Button>
                      </LinkContainer>
                    )}>
          <span>
            Content packs accelerate the set up process for a specific data source. A content pack can include inputs/extractors, streams, and dashboards.
            <br />
            Find more content packs in {' '}
            <a href="https://marketplace.graylog.org/" target="_blank" rel="noopener noreferrer">the Graylog Marketplace</a>.
          </span>
        </PageHeader>
        <ContentPackEdit contentPack={contentPack}
                         onGetEntities={_getEntities}
                         onStateChange={_onStateChanged}
                         fetchedEntities={fetchedEntities}
                         selectedEntities={selectedEntities}
                         entityIndex={entityCatalog}
                         appliedParameter={appliedParameter}
                         edit
                         onSave={_onSave} />
      </span>
    </DocumentTitle>
  );
};

export default EditContentPackPage;
