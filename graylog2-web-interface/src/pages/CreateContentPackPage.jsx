import React from 'react';
import Reflux from 'reflux';
import createReactClass from 'create-react-class';

import Routes from 'routing/Routes';
import { Button } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';
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
      selectedStep: undefined,
      entityIndex: undefined,
    };
  },

  componentDidMount() {
    CatalogActions.showEntityIndex();
  },

  _getUUID() {
    const s4 = () => {
      return Math.floor((1 + Math.random()) * 0x10000)
        .toString(16)
        .substring(1);
    };
    return `${s4()}${s4()}-${s4()}-${s4()}-${s4()}-${s4()}${s4()}${s4()}`;
  },

  _onStateChanged(newState) {
    const contentPack = newState.contentPack || this.state.contentPack;
    const selectedEntities = newState.selectedEntities || this.state.selectedEntities;
    const appliedParameter = newState.appliedParameter || this.state.appliedParameter;
    this.setState({
      contentPack: contentPack,
      selectedEntities: selectedEntities,
      appliedParameter: appliedParameter,
    });
  },

  _onSave() {
    ContentPacksActions.create.triggerPromise(this.state.contentPack.toJSON())
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
    CatalogActions.getSelectedEntities(selectedEntities).then((result) => {
      const newContentPack = this.state.contentPack.toBuilder()
        /* Mark entities from server */
        .entities(result.entities.map(e => Entity.fromJSON(e, true, this.state.contentPack.parameters)))
        .build();
      const fetchedEntities = result.entities.map(e => Entity.fromJSON(e, false, this.state.contentPack.parameters));
      this.setState({ contentPack: newContentPack, fetchedEntities });
    });
  },

  render() {
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
          <ContentPackEdit contentPack={this.state.contentPack}
                           onGetEntities={this._getEntities}
                           onStateChange={this._onStateChanged}
                           fetchedEntities={this.state.fetchedEntities}
                           selectedEntities={this.state.selectedEntities}
                           appliedParameter={this.state.appliedParameter}
                           entityIndex={this.state.entityIndex}
                           onSave={this._onSave} />
        </span>
      </DocumentTitle>
    );
  },
});

export default CreateContentPackPage;
