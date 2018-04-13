import React from 'react';
import Reflux from 'reflux';
import createReactClass from 'create-react-class';

import Routes from 'routing/Routes';
import { Button } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';

import { AutoAffix } from 'react-overlays';
import UserNotification from 'util/UserNotification';
import { DocumentTitle, PageHeader } from 'components/common';
import Wizard from 'components/common/Wizard';
import ContentPackSelection from 'components/content-packs/ContentPackSelection';
import ContentPackDependencyResolution from 'components/content-packs/ContentPackDependencyResolution';
import ContentPackDetails from 'components/content-packs/ContentPackDetails';
import CombinedProvider from 'injection/CombinedProvider';
import ContentPackPreview from 'components/content-packs/ContentPackPreview';
import ContentPackParameters from 'components/content-packs/ContentPackParameters';

const { ContentPacksActions } = CombinedProvider.get('ContentPacks');
const { CatalogActions, CatalogStore } = CombinedProvider.get('Catalog');

const CreateContentPackPage = createReactClass({
  displayName: 'ShowContentPackPage',
  mixins: [Reflux.connect(CatalogStore)],

  getInitialState() {
    return {
      contentPack: {
        v: 1,
        id: this._getUUID(),
        rev: 1,
        requires: [],
        parameters: [],
        entities: [],
      },
      selectedEntities: {},
      selectedStep: undefined,
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
    this.setState({
      contentPack: contentPack,
      selectedEntities: selectedEntities,
    });
  },

  _onSave() {
    ContentPacksActions.create.triggerPromise(this.state.contentPack)
      .then(
        () => {
          UserNotification.success('Content pack imported successfully', 'Success!');
        },
        (response) => {
          const message = 'Error importing content pack, please ensure it is a valid JSON file. Check your ' +
            'Graylog logs for more information.';
          const title = 'Could not import content pack';
          let smallMessage = '';
          if (response.additional && response.additional.body && response.additional.body.message) {
            smallMessage = `<br /><small>${response.additional.body.message}</small>`;
          }
          UserNotification.error(message + smallMessage, title);
        });
  },

  _selectionComponent() {
    return (
      <ContentPackSelection contentPack={this.state.contentPack}
                            selectedEntities={this.state.selectedEntities}
                            onStateChange={this._onStateChanged}
                            entities={this.state.entityIndex}
      />
    );
  },

  _dependencyComponent() {
    return (
      <ContentPackDependencyResolution contentPack={this.state.contentPack} />
    )
  },

  _disableDependencyRes() {
    const content = this.state.contentPack;
    const selection = Object.keys(this.state.selectedEntities).length !== 0;
    return !(content.name && content.summary && content.description && content.vendor &&
        selection);
  },

  render() {
    const steps = [
      { key: 'selection', title: 'Content Selection', component: (this._selectionComponent()) },
      { key: 'dependency', title: 'Dependency Resolution', component: (this._dependencyComponent()), disabled: this._disableDependencyRes() },
      { key: 'parameters', title: 'Parameters', component: (<ContentPackParameters contentPack={this.state.contentPack} onStateChange={this._onStateChanged} />), disabled: this._disableDependencyRes() },
      { key: 'preview', title: 'Preview', component: (<ContentPackPreview contentPack={this.state.contentPack} onSave={this._onSave} />), disabled: this._disableDependencyRes() },
    ];

    return (
      <DocumentTitle title="Content packs">
        <span>
          <PageHeader title="Create content packs">
            <span>
              Content packs accelerate the set up process for a specific data source. A content pack can include inputs/extractors, streams, and dashboards.
            </span>

            <span>
              Find more content packs in {' '}
              <a href="https://marketplace.graylog.org/" target="_blank">the Graylog Marketplace</a>.
            </span>

            <div>
              <LinkContainer to={Routes.SYSTEM.CONTENTPACKS.LIST}>
                <Button bsStyle="info" bsSize="large">Content Packs</Button>
              </LinkContainer>
            </div>
          </PageHeader>
          <Wizard steps={steps}>
            <AutoAffix viewportOffsetTop={65}>
              <div>
                <ContentPackDetails contentPack={this.state.contentPack} />
              </div>
            </AutoAffix>
          </Wizard>
          <textarea value={JSON.stringify(this.state.contentPack)} />
          <textarea value={JSON.stringify(this.state.selectedEntities)} />
        </span>
      </DocumentTitle>
    );
  },
});

export default CreateContentPackPage;
