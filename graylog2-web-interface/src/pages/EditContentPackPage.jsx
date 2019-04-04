import React from 'react';
import PropTypes from 'prop-types';
import Reflux from 'reflux';
import createReactClass from 'create-react-class';
import { cloneDeep, groupBy } from 'lodash';

import Routes from 'routing/Routes';
import { Button } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';
import history from 'util/History';

import UserNotification from 'util/UserNotification';
import { DocumentTitle, PageHeader } from 'components/common';
import CombinedProvider from 'injection/CombinedProvider';
import ValueReferenceData from 'util/ValueReferenceData';
import ContentPackEdit from 'components/content-packs/ContentPackEdit';

import Entity from 'logic/content-packs/Entity';

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
      selectedStep: undefined,
      contentPackEntities: undefined,
      appliedParameter: {},
      entityCatalog: {},
    };
  },

  componentDidMount() {
    ContentPacksActions.get(this.props.params.contentPackId).then(() => {
      const originContentPackRev = this.props.params.contentPackRev;
      const newContentPack = this.state.contentPackRevisions.createNewVersionFromRev(originContentPackRev);
      this.setState({ contentPack: newContentPack, contentPackEntities: cloneDeep(newContentPack.entities) });

      CatalogActions.showEntityIndex().then(() => {
        this._createEntityCatalog();
        this._getSelectedEntities();
        this._getAppliedParameter();
      });
    });
  },

  _createEntityCatalog() {
    if (!this.state.contentPack || !this.state.entityIndex) {
      return;
    }
    const groupedContentPackEntities = groupBy(this.state.contentPackEntities, 'type.name');
    const entityCatalog = Object.keys(this.state.entityIndex)
      .reduce((result, entityType) => {
        /* eslint-disable-next-line no-param-reassign */
        result[entityType] = this.state.entityIndex[entityType].concat(groupedContentPackEntities[entityType] || []);
        return result;
      }, {});
    this.setState({ entityCatalog });
  },

  _getSelectedEntities() {
    if (!this.state.contentPack || !this.state.entityIndex) {
      return;
    }
    const selectedEntities = this.state.contentPack.entities.reduce((result, entity) => {
      if (this.state.entityCatalog[entity.type.name]
        && this.state.entityCatalog[entity.type.name].findIndex((fetchedEntity) => { return fetchedEntity.id === entity.id; }) >= 0) {
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
    const appliedParameter = this.state.contentPack.entities.reduce((result, entity) => {
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
      const contentPackEntities = Object.keys(selectedEntities)
        .reduce((acc, entityType) => {
          return acc.concat(selectedEntities[entityType]);
        }, []).filter(e => e instanceof Entity);
      /* Mark entities from server */
      const entities = contentPackEntities.concat(result.entities.map(e => Entity.fromJSON(e, true)));
      const contentPack = this.state.contentPack.toBuilder()
        .entities(entities)
        .build();
      this.setState({ contentPack: contentPack, fetchedEntities: contentPack.entities });
    });
  },

  render() {
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
          <ContentPackEdit contentPack={this.state.contentPack}
                           onGetEntities={this._getEntities}
                           onStateChange={this._onStateChanged}
                           fetchedEntities={this.state.fetchedEntities}
                           selectedEntities={this.state.selectedEntities}
                           entityIndex={this.state.entityCatalog}
                           appliedParameter={this.state.appliedParameter}
                           edit
                           onSave={this._onSave} />
        </span>
      </DocumentTitle>
    );
  },
});

export default EditContentPackPage;
