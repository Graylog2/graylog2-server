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
import PropTypes from 'prop-types';
import React from 'react';

import { Button, Modal, ButtonToolbar } from 'components/bootstrap';
import { SearchForm, DataTable, Icon } from 'components/common';
import BootstrapModalWrapper from 'components/bootstrap/BootstrapModalWrapper';

import ContentPackApplyParameter from './ContentPackApplyParameter';
import ContentPackEntityConfig from './ContentPackEntityConfig';
import ContentPackEntitiesListStyle from './ContentPackEntitiesList.css';

const EntityIcon = ({ entity }) => {
  if (!entity.fromServer) {
    return <span><Icon title="Content Pack" name="archive" className={ContentPackEntitiesListStyle.contentPackEntity} /></span>;
  }

  return <span><Icon title="Server" name="server" /></span>;
};

EntityIcon.propTypes = {
  entity: PropTypes.object.isRequired,
};

class ContentPackEntitiesList extends React.Component {
  static propTypes = {
    contentPack: PropTypes.object.isRequired,
    appliedParameter: PropTypes.object,
    onParameterApply: PropTypes.func,
    onParameterClear: PropTypes.func,
    readOnly: PropTypes.bool,
  };

  static defaultProps = {
    appliedParameter: {},
    onParameterClear: () => {},
    onParameterApply: () => {},
    readOnly: false,
  };

  constructor(props) {
    super(props);

    this.state = {
      showApplyConfigModal: false,
      showConfigModalId: undefined,
      filteredEntities: props.contentPack.entities || [],
      filter: undefined,
    };
  }

  UNSAFE_componentWillReceiveProps(newProps) {
    const { filter } = this.state;

    this._filterEntities(filter, newProps.contentPack.entities);
  }

  _filterEntities = (filter, entitiesArg) => {
    const { contentPack } = this.props;

    const entities = entitiesArg || contentPack.entities;

    if (!filter || filter.length <= 0) {
      this.setState({ filteredEntities: entities, filter: undefined });

      return;
    }

    const regexp = RegExp(filter, 'i');
    const filteredEntities = entities.filter((entity) => {
      return regexp.test(entity.title) || regexp.test(entity.description);
    });

    this.setState({ filteredEntities: filteredEntities, filter: filter });
  };

  _entityRowFormatter = (entity) => {
    const {
      contentPack,
      appliedParameter,
      onParameterApply,
      onParameterClear,
      readOnly,
    } = this.props;

    const applyParamComponent = (
      <ContentPackApplyParameter parameters={contentPack.parameters}
                                 entity={entity}
                                 appliedParameter={appliedParameter[entity.id]}
                                 onParameterApply={(key, value) => { onParameterApply(entity.id, key, value); }}
                                 onParameterClear={(key) => { onParameterClear(entity.id, key); }} />
    );

    const closeModal = () => {
      this.setState({ showApplyConfigModal: false });
    };

    const open = () => {
      this.setState({ showApplyConfigModal: true });
    };

    const applyModal = (
      <BootstrapModalWrapper showModal={this.state.showApplyConfigModal}
                             onHide={closeModal}
                             bsSize="large"
                             data-app-section="content_pack_entity_list"
                             data-event-element="Edit">
        <Modal.Header closeButton>
          <Modal.Title>Edit</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          {applyParamComponent}
        </Modal.Body>
        <Modal.Footer>
          <Button onClick={closeModal}>Close</Button>
        </Modal.Footer>
      </BootstrapModalWrapper>
    );

    const entityComponent = (
      <ContentPackEntityConfig appliedParameter={appliedParameter[entity.id]}
                               parameters={contentPack.parameters}
                               entity={entity} />
    );

    const closeShowModal = () => {
      this.setState({ showConfigModalId: undefined });
    };

    const openShowModal = (entityId) => {
      this.setState({ showConfigModalId: entityId });
    };

    const showModal = this.state.showConfigModalId && (
      <BootstrapModalWrapper showModal={entity.id === this.state.showConfigModalId}
                             onHide={closeShowModal}
                             bsSize="large"
                             data-app-section="content_pack_entity_list"
                             data-event-element="Entity Config">
        <Modal.Header closeButton>
          <Modal.Title>Entity Config</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          {entityComponent}
        </Modal.Body>
        <Modal.Footer>
          <Button onClick={closeShowModal}>Close</Button>
        </Modal.Footer>
      </BootstrapModalWrapper>
    );

    const disableBtn = contentPack.parameters.length <= 0;
    const appliedParameterCount = (appliedParameter[entity.id] || []).length;

    return (
      <tr key={entity.id}>
        <td className={ContentPackEntitiesListStyle.bigColumns}>{entity.title}</td>
        <td>{entity.type.name}</td>
        <td className={ContentPackEntitiesListStyle.bigColumns}>{entity.description}</td>
        {!readOnly && <td><EntityIcon entity={entity} /></td>}
        {!readOnly && <td>{appliedParameterCount}</td>}
        <td>
          <ButtonToolbar>
            {!readOnly
            && (
            <Button bsStyle="primary"
                    bsSize="xs"
                    disabled={disableBtn}
                    onClick={() => {
                      open();
                    }}>
              Edit
            </Button>
            )}
            <Button bsSize="xs"
                    onClick={() => { openShowModal(entity.id); }}>
              Show
            </Button>
          </ButtonToolbar>
        </td>
        {!readOnly && applyModal}
        {showModal}
      </tr>
    );
  };

  render() {
    const { readOnly } = this.props;
    const { filteredEntities } = this.state;

    const headers = readOnly
      ? ['Title', 'Type', 'Description', 'Action']
      : ['Title', 'Type', 'Description', 'Origin', 'Used Parameters', 'Action'];

    return (
      <div>
        <h2>Entity list</h2>
        <br />
        <SearchForm onSearch={this._filterEntities}
                    onReset={() => { this._filterEntities(''); }} />
        <DataTable id="entity-list"
                   headers={headers}
                   className={ContentPackEntitiesListStyle.scrollable}
                   sortBy={(entity) => entity.type.name}
                   filterKeys={[]}
                   rows={filteredEntities}
                   dataRowFormatter={this._entityRowFormatter} />
      </div>
    );
  }
}

export default ContentPackEntitiesList;
