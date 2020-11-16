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
import Reflux from 'reflux';
import createReactClass from 'create-react-class';

import { LinkContainer } from 'components/graylog/router';
import Routes from 'routing/Routes';
import { Button } from 'components/graylog';
import history from 'util/History';
import UserNotification from 'util/UserNotification';
import { DocumentTitle, PageHeader } from 'components/common';
import CombinedProvider from 'injection/CombinedProvider';
import ContentPackEdit from 'components/content-packs/ContentPackEdit';
import ContentPack from 'logic/content-packs/ContentPack';
import Entity from 'logic/content-packs/Entity';

const { ContentPacksActions } = CombinedProvider.get('ContentPacks');
const { CatalogActions, CatalogStore } = CombinedProvider.get('Catalog');

const CreateContentPackPage = createReactClass({
  displayName: 'CreateContentPackPage',
  mixins: [Reflux.connect(CatalogStore)],

  getInitialState() {
    return {
      contentPack: ContentPack.builder().build(),
      appliedParameter: {},
      selectedEntities: {},
      entityIndex: undefined,
    };
  },

  componentDidMount() {
    CatalogActions.showEntityIndex();
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
      const newContentPack = contentPack.toBuilder()
        /* Mark entities from server */
        .entities(result.entities.map((e) => Entity.fromJSON(e, true, contentPack.parameters)))
        .build();
      const fetchedEntities = result.entities.map((e) => Entity.fromJSON(e, false, contentPack.parameters));

      this.setState({ contentPack: newContentPack, fetchedEntities });
    });
  },

  render() {
    const { contentPack, fetchedEntities, selectedEntities, appliedParameter, entityIndex } = this.state;

    return (
      <DocumentTitle title="Content packs">
        <span>
          <PageHeader title="Create content packs">
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
                           appliedParameter={appliedParameter}
                           entityIndex={entityIndex}
                           onSave={this._onSave} />
        </span>
      </DocumentTitle>
    );
  },
});

export default CreateContentPackPage;
