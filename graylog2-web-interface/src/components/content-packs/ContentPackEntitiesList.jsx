import PropTypes from 'prop-types';
import React from 'react';

import { Button, Modal, ButtonToolbar } from 'react-bootstrap';
import DataTable from 'components/common/DataTable';
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
          <Modal.Title>Apply Parameter</Modal.Title>
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
        <td>{(entity.data.title || {}).value}</td>
        <td>{entity.type}</td>
        <td>{(entity.data.description || entity.data.name || {}).value}</td>
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
              Apply Parameter
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
      ['Title', 'Type', 'Description', 'Applied Parameter', 'Action'];

    return (
      <div>
        <h2>Entity list</h2>
        <br />
        <DataTable
          id="entity-list"
          headers={headers}
          className={ContentPackEntitiesListStyle.scrollable}
          sortByKey="type"
          filterKeys={[]}
          rows={this.props.contentPack.entities}
          dataRowFormatter={this._entityRowFormatter}
        />
      </div>
    );
  }
}

export default ContentPackEntitiesList;
