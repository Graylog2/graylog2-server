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
import PropTypes from 'prop-types';
import Reflux from 'reflux';
import createReactClass from 'create-react-class';
import { cloneDeep, groupBy } from 'lodash';

import { LinkContainer } from 'components/graylog/router';
import Routes from 'routing/Routes';
import { Button } from 'components/graylog';
import history from 'util/History';
import UserNotification from 'util/UserNotification';
import { DocumentTitle, PageHeader } from 'components/common';
import CombinedProvider from 'injection/CombinedProvider';
import ValueReferenceData from 'util/ValueReferenceData';
import ContentPackEdit from 'components/content-packs/ContentPackEdit';
import Entity from 'logic/content-packs/Entity';
import withParams from 'routing/withParams';

const { CatalogActions, CatalogStore } = CombinedProvider.get('Catalog');
const { ContentPacksActions, ContentPacksStore } = CombinedProvider.get('ContentPacks');

const EditContentPackPage = createReactClass({
  displayName: 'EditContentPackPage',

  propTypes: {
    params: PropTypes.object.isRequired,
  },

  mixins: [Reflux.connect(CatalogStore), Reflux.connect(ContentPacksStore)],

  getInitialState() {
    return {
      selectedEntities: {},
      contentPackEntities: undefined,
      appliedParameter: {},
      entityCatalog: {},
    };
  },

  componentDidMount() {
    const { params } = this.props;

    ContentPacksActions.get(params.contentPackId).then((result) => {
      const { contentPackRevisions } = result;
      const originContentPackRev = params.contentPackRev;
      const newContentPack = contentPackRevisions.createNewVersionFromRev(originContentPackRev);

      this.setState({ contentPack: newContentPack, contentPackEntities: cloneDeep(newContentPack.entities) });

      CatalogActions.showEntityIndex().then(() => {
        this._createEntityCatalog();
        this._getSelectedEntities();
        this._getAppliedParameter();
      });
    });
  },

  _createEntityCatalog() {
    const { contentPack, contentPackEntities, entityIndex } = this.state;

    if (!contentPack || !entityIndex) {
      return;
    }

    const groupedContentPackEntities = groupBy(contentPackEntities, 'type.name');
    const entityCatalog = Object.keys(entityIndex)
      .reduce((result, entityType) => {
        /* eslint-disable-next-line no-param-reassign */
        result[entityType] = entityIndex[entityType].concat(groupedContentPackEntities[entityType] || []);

        return result;
      }, {});

    this.setState({ entityCatalog });
  },

  _getSelectedEntities() {
    const { contentPack, entityCatalog, entityIndex } = this.state;

    if (!contentPack || !entityIndex) {
      return;
    }

    const selectedEntities = contentPack.entities.reduce((result, entity) => {
      if (entityCatalog[entity.type.name]
        && entityCatalog[entity.type.name].findIndex((fetchedEntity) => { return fetchedEntity.id === entity.id; }) >= 0) {
        const newResult = result;

        newResult[entity.type.name] = result[entity.type.name] || [];
        newResult[entity.type.name].push(entity);

        return newResult;
      }

      return result;
    }, {});

    this.setState({ selectedEntities: selectedEntities });
  },

  _getAppliedParameter() {
    const { contentPack } = this.state;

    const appliedParameter = contentPack.entities.reduce((result, entity) => {
      const entityData = new ValueReferenceData(entity.data);
      const configPaths = entityData.getPaths();

      const paramMap = Object.keys(configPaths).filter((path) => {
        return configPaths[path].isValueParameter();
      }).map((path) => {
        return { configKey: path, paramName: configPaths[path].getValue(), readOnly: true };
      });
      const newResult = result;

      if (paramMap.length > 0) {
        newResult[entity.id] = paramMap;
      }

      return newResult;
    }, {});

    this.setState({ appliedParameter: appliedParameter });
  },

  _onStateChanged(newState) {
    const { contentPack, selectedEntities, appliedParameter } = this.state;

    this.setState({
      contentPack: newState.contentPack || contentPack,
      selectedEntities: newState.selectedEntities || selectedEntities,
      appliedParameter: newState.appliedParameter || appliedParameter,
    });
  },

  _onSave() {
    const { contentPack } = this.state;

    ContentPacksActions.create.triggerPromise(contentPack.toJSON())
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
  },

  _getEntities(selectedEntities) {
    const { contentPack } = this.state;

    CatalogActions.getSelectedEntities(selectedEntities).then((result) => {
      const contentPackEntities = Object.keys(selectedEntities)
        .reduce((acc, entityType) => {
          return acc.concat(selectedEntities[entityType]);
        }, []).filter((e) => e instanceof Entity);
      /* Mark entities from server */
      const entities = contentPackEntities.concat(result.entities.map((e) => Entity.fromJSON(e, true)));
      const builtContentPack = contentPack.toBuilder()
        .entities(entities)
        .build();

      this.setState({ contentPack: builtContentPack, fetchedEntities: builtContentPack.entities });
    });
  },

  render() {
    const { contentPack, fetchedEntities, selectedEntities, entityCatalog, appliedParameter } = this.state;

    return (
      <DocumentTitle title="Content packs">
        <span>
          <PageHeader title="Edit content pack">
            <span>
              Content packs accelerate the set up process for a specific data source. A content pack can include inputs/extractors, streams, and dashboards.
            </span>

            <span>
              Find more content packs in {' '}
              <a href="https://marketplace.graylog.org/" target="_blank" rel="noopener noreferrer">the Graylog Marketplace</a>.
            </span>

            <div>
              <LinkContainer to={Routes.SYSTEM.CONTENTPACKS.LIST}>
                <Button bsStyle="info">Content Packs</Button>
              </LinkContainer>
            </div>
          </PageHeader>
          <ContentPackEdit contentPack={contentPack}
                           onGetEntities={this._getEntities}
                           onStateChange={this._onStateChanged}
                           fetchedEntities={fetchedEntities}
                           selectedEntities={selectedEntities}
                           entityIndex={entityCatalog}
                           appliedParameter={appliedParameter}
                           edit
                           onSave={this._onSave} />
        </span>
      </DocumentTitle>
    );
  },
});

export default withParams(EditContentPackPage);
