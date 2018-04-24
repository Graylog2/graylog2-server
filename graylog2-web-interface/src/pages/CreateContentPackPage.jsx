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
import ContentPackDetails from 'components/content-packs/ContentPackDetails';
import CombinedProvider from 'injection/CombinedProvider';
import ContentPackPreview from 'components/content-packs/ContentPackPreview';
import ContentPackParameters from 'components/content-packs/ContentPackParameters';
import ObjectUtils from 'util/ObjectUtils';

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
      appliedParameter: {},
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

  _prepareForPreview() {
    const newContentPack = ObjectUtils.clone(this.state.contentPack);
    const entities = ObjectUtils.clone(this.state.fetchedEntities);
    const preparedEntities = entities.map((entity) => {
      const parameters = this.state.appliedParameter[entity.id] || [];
      const entityData = ObjectUtils.clone(entity.data);
      const configKeys = ObjectUtils.getPaths(entityData);
      configKeys.forEach((path) => {
        const index = parameters.findIndex((paramMap) => { return paramMap.configKey === path });
        let newValue;
        if (index >= 0) {
          newValue = { type: 'parameter', value: parameters[index].paramName };
        } else {
          const currentValue = ObjectUtils.getValue(entityData, path);
          newValue = { type: 'value', value: currentValue };
        }
        ObjectUtils.setValue(entityData, path, newValue);
      });
      return entityData;
    });
    newContentPack.entities = preparedEntities;
    this.setState({ contentPack: newContentPack });
  },

  _stepChanged(selectedStep) {
    switch (selectedStep) {
      case 'parameters': {
        const newContentPack = ObjectUtils.clone(this.state.contentPack);
        newContentPack.entities = this.state.fetchedEntities || [];
        this.setState({contentPack: newContentPack});
        if (Object.keys(this.state.selectedEntities).length > 0) {
          CatalogActions.getSelectedEntities(this.state.selectedEntities).then((fetchedEntities) => {
            const contentPack = ObjectUtils.clone(this.state.contentPack);
            contentPack.entities = fetchedEntities;
            this.setState({contentPack: contentPack});
          });
        }
        break;
      }
      case 'preview': {
        this._prepareForPreview();
        break;
      }
      default: {
        break;
      }
    }
    this.setState({ selectedStep: selectedStep });
  },

  _disableNextStep() {
    const content = this.state.contentPack;
    const selection = Object.keys(this.state.selectedEntities).length !== 0;
    return !(content.name && content.summary && content.description && content.vendor &&
        selection);
  },

  render() {
    const steps = [
      { key: 'selection', title: 'Content Selection', component: (this._selectionComponent()) },
      { key: 'parameters', title: 'Parameters', component: (<ContentPackParameters contentPack={this.state.contentPack} onStateChange={this._onStateChanged} appliedParameter={this.state.appliedParameter} />), disabled: this._disableNextStep() },
      { key: 'preview', title: 'Preview', component: (<ContentPackPreview contentPack={this.state.contentPack} onSave={this._onSave} />), disabled: this._disableNextStep() },
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
          <Wizard steps={steps} onStepChange={this._stepChanged}>
            <AutoAffix viewportOffsetTop={65}>
              <div>
                {this.state.selectedStep !== 'preview' && <ContentPackDetails contentPack={this.state.contentPack} />}
              </div>
            </AutoAffix>
          </Wizard>
        </span>
      </DocumentTitle>
    );
  },
});

export default CreateContentPackPage;
