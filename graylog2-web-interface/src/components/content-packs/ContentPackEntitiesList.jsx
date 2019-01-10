import PropTypes from 'prop-types';
import React from 'react';

import { Button, Modal, ButtonToolbar } from 'react-bootstrap';
import { SearchForm, DataTable } from 'components/common';
import BootstrapModalWrapper from 'components/bootstrap/BootstrapModalWrapper';

import ContentPackApplyParameter from './ContentPackApplyParameter';
import ContentPackEntityConfig from './ContentPackEntityConfig';

import ContentPackEntitiesListStyle from './ContentPackEntitiesList.css';

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
      filteredEntities: props.contentPack.entities || [],
      filter: undefined,
    };
  }

  componentWillReceiveProps(newProps) {
    this._filterEntities(this.state.filter, newProps.contentPack.entities);
  }

  _filterEntities = (filter, entitiesArg) => {
    const entities = entitiesArg || this.props.contentPack.entities;
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

  _entityIcon = (entity) => {
    if (!entity.fromServer) {
      return <span><i title="Content Pack" className={`fa fa-archive ${ContentPackEntitiesListStyle.contentPackEntity}`} /></span>;
    }
    return <span><i title="Server" className="fa fa-server" /></span>;
  };

  _entityRowFormatter = (entity) => {
    let applyModalRef;
    const applyParamComponent = (<ContentPackApplyParameter
      parameters={this.props.contentPack.parameters}
      entity={entity}
      appliedParameter={this.props.appliedParameter[entity.id]}
      onParameterApply={(key, value) => { this.props.onParameterApply(entity.id, key, value); }}
      onParameterClear={(key) => { this.props.onParameterClear(entity.id, key); }}
    />);

    const closeModal = () => {
      applyModalRef.close();
    };

    const open = () => {
      applyModalRef.open();
    };

    const applyModal = (
      <BootstrapModalWrapper ref={(node) => { applyModalRef = node; }} bsSize="large">
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

    let showModalRef;
    const entityComponent = (<ContentPackEntityConfig
      appliedParameter={this.props.appliedParameter[entity.id]}
      parameters={this.props.contentPack.parameters}
      entity={entity}
    />);

    const closeShowModal = () => {
      showModalRef.close();
    };

    const openShowModal = () => {
      showModalRef.open();
    };

    const showModal = (
      <BootstrapModalWrapper ref={(node) => { showModalRef = node; }} bsSize="large">
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

    const disableBtn = this.props.contentPack.parameters.length <= 0;
    const appliedParameterCount = (this.props.appliedParameter[entity.id] || []).length;
    return (
      <tr key={entity.id}>
        <td className={ContentPackEntitiesListStyle.bigColumns}>{entity.title}</td>
        <td>{entity.type.name}</td>
        <td className={ContentPackEntitiesListStyle.bigColumns}>{entity.description}</td>
        {!this.props.readOnly && <td>{this._entityIcon(entity)}</td>}
        {!this.props.readOnly && <td>{appliedParameterCount}</td>}
        <td>
          <ButtonToolbar>
            {!this.props.readOnly &&
            <Button bsStyle="primary"
                    bsSize="xs"
                    disabled={disableBtn}
                    onClick={() => {
                      open();
                    }}>
              Edit
            </Button>
            }
            <Button bsStyle="info"
                    bsSize="xs"
                    onClick={() => { openShowModal(); }}>
              Show
            </Button>
          </ButtonToolbar>
        </td>
        {!this.props.readOnly && applyModal}
        {showModal}
      </tr>
    );
  };

  render() {
    const headers = this.props.readOnly ?
      ['Title', 'Type', 'Description', 'Action'] :
      ['Title', 'Type', 'Description', 'Origin', 'Used Parameters', 'Action'];

    return (
      <div>
        <h2>Entity list</h2>
        <br />
        <SearchForm
          searchButtonLabel="Filter"
          onSearch={this._filterEntities}
          onReset={() => { this._filterEntities(''); }}
        />
        <DataTable
          id="entity-list"
          headers={headers}
          className={ContentPackEntitiesListStyle.scrollable}
          sortBy={entity => entity.type.name}
          filterKeys={[]}
          rows={this.state.filteredEntities}
          dataRowFormatter={this._entityRowFormatter}
        />
      </div>
    );
  }
}

export default ContentPackEntitiesList;
