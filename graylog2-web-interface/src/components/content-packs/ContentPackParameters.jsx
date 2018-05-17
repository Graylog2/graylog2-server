import PropTypes from 'prop-types';
import React from 'react';
import lodash from 'lodash';

import { Row, Col, Button, Modal } from 'react-bootstrap';
import DataTable from 'components/common/DataTable';
import ObjectUtils from 'util/ObjectUtils';
import BootstrapModalWrapper from 'components/bootstrap/BootstrapModalWrapper';

import ContentPackApplyParameter from 'components/content-packs/ContentPackApplyParameter';
import ContentPackEditParameter from 'components/content-packs/ContentPackEditParameter';
import ContentPackEntityConfig from "./ContentPackEntityConfig";

class ContentPackParameters extends React.Component {
  static propTypes = {
    contentPack: PropTypes.object.isRequired,
    onStateChange: PropTypes.func,
    appliedParameter: PropTypes.object.isRequired,
  };

  static defaultProps = {
    onStateChange: () => {},
  };

  static emptyParameter = {
    name: '',
    title: '',
    description: '',
    type: 'string',
    default_value: '',
  };

  constructor(props) {
    super(props);
    this.state = {
      newParameter: ObjectUtils.clone(ContentPackParameters.emptyParameter),
      defaultValueError: undefined,
      nameError: undefined,
    };
  }

  _addNewParameter = (newParameter, oldParameter) => {
    const newContentPack = ObjectUtils.clone(this.props.contentPack);
    const newAppliedParameter = ObjectUtils.clone(this.props.appliedParameter);
    if (oldParameter) {
      /* If the name of the parameter changed we need to update the reference in appliedParameter */
      Object.keys(newAppliedParameter).forEach((id) => {
        newAppliedParameter[id] = newAppliedParameter[id].map((paramMap) => {
          if (paramMap.paramName === oldParameter.name) {
            return { configKey: paramMap.configKey, paramName: newParameter.name };
          }
          return paramMap;
        });
      });
      /* If we update a parameter we remove the old one first */
      lodash.remove(newContentPack.parameters, (parameter) => {
        return parameter.name === oldParameter.name;
      });
    }
    newContentPack.parameters.push(newParameter);
    this.props.onStateChange({ contentPack: newContentPack, appliedParameter: newAppliedParameter });
  };

  _onParameterApply = (id, configKey, paramName) => {
    const paramMap = { configKey: configKey, paramName: paramName };
    const newAppliedParameter = ObjectUtils.clone(this.props.appliedParameter);
    newAppliedParameter[id] = newAppliedParameter[id] || [];
    newAppliedParameter[id].push(paramMap);
    this.props.onStateChange({ appliedParameter: newAppliedParameter });
  };

  _onParameterClear = (id, configKey) => {
    const newAppliedParameter = ObjectUtils.clone(this.props.appliedParameter);
    lodash.remove(newAppliedParameter[id], (paramMap) => { return paramMap.configKey === configKey; });
    this.props.onStateChange({ appliedParameter: newAppliedParameter });
  };

  _deleteParameter = (parameter) => {
    const newContentPack = ObjectUtils.clone(this.props.contentPack);
    const newAppliedParameter = ObjectUtils.clone(this.props.appliedParameter);
    /* If we delete a parameter we need to remove the reference from appliedParameter */
    Object.keys(newAppliedParameter).forEach((id) => {
      lodash.remove(newAppliedParameter[id], (paramMap) => { return paramMap.paramName === parameter.name; });
      if (newAppliedParameter[id].length <= 0) {
        delete newAppliedParameter[id];
      }
    });
    lodash.remove(newContentPack.parameters, (param) => { return param.name === parameter.name; });
    this.props.onStateChange({ contentPack: newContentPack, appliedParameter: newAppliedParameter });
  };

  _parameterRowFormatter = (parameter) => {
    return (
      <tr key={parameter.title}>
        <td>{parameter.title}</td>
        <td>{parameter.name}</td>
        <td>{parameter.description}</td>
        <td>{parameter.type}</td>
        <td>{parameter.default_value}</td>
        <td>
          <Button bsStyle="primary" bsSize="small" onClick={() => { this._deleteParameter(parameter); }}>Delete</Button>{this._parameterModal(parameter)}
        </td>
      </tr>
    );
  };

  _entityRowFormatter = (entity) => {
    let applyModalRef;
    const applyParamComponent = (<ContentPackApplyParameter
      parameters={this.props.contentPack.parameters}
      entity={entity}
      appliedParameter={this.props.appliedParameter[entity.id]}
      onParameterApply={(key, value) => { this._onParameterApply(entity.id, key, value); }}
      onParameterClear={(key) => { this._onParameterClear(entity.id, key); }}
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
          <Modal.Title>Apply Parameter</Modal.Title>
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
        <td>{appliedParameterCount}</td>
        <td>
          <Button bsStyle="primary"
                  bsSize="small"
                  disabled={disableBtn}
                  onClick={() => { open(); }}>
            Apply Parameter
          </Button>
          <Button bsStyle="info"
                  bsSize="small"
                  onClick={() => { openShowModal(); }}>
            Show config
          </Button>
        </td>
        {applyModal}
        {showModal}
      </tr>
    );
  };

  _parameterModal(parameter) {
    let modalRef;

    const closeModal = () => {
      modalRef.close();
    };

    const open = () => {
      modalRef.open();
    };
    const size = parameter ? 'small' : 'small';
    const name = parameter ? 'Edit' : 'Create parameter';

    const modal = (
      <BootstrapModalWrapper ref={(node) => { modalRef = node; }} bsSize="large">
        <Modal.Header closeButton>
          <Modal.Title>Parameter</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <ContentPackEditParameter parameters={this.props.contentPack.parameters}
                                    onUpdateParameter={(newParameter) => {
                                      this._addNewParameter(newParameter, parameter);
                                      if (parameter) {
                                        closeModal();
                                      }
                                    }}
                                    parameterToEdit={parameter} />
        </Modal.Body>
        <Modal.Footer>
          <Button onClick={closeModal}>Close</Button>
        </Modal.Footer>
      </BootstrapModalWrapper>
    );

    return (
      <span>
        <Button bsStyle="info"
                bsSize={size}
                onClick={() => { open(); }}>
          {name}
        </Button>
        {modal}
      </span>
    );
  }

  render() {
    return (
      <div>
        <Row>
          <Col smOffset={1} sm={9}>
            <h2>Parameters list</h2>
            <br />
            {this._parameterModal()}
            <br />
            <br />
            <DataTable
              id="parameter-list"
              headers={['Title', 'Name', 'Description', 'Value Type', 'Default Value', 'Action']}
              sortByKey="title"
              noDataText="To use parameters for content packs, at first a parameter must be created and can then be applied to a entity."
              filterKeys={[]}
              rows={this.props.contentPack.parameters}
              dataRowFormatter={this._parameterRowFormatter}
            />
          </Col>
        </Row>
        <Row>
          <Col smOffset={1} sm={9}>
            <h2>Entity list</h2>
            <br />
            <DataTable
              id="entity-list"
              headers={['Title', 'Type', 'Description', 'Applied Parameter', 'Action']}
              sortByKey="type"
              filterKeys={[]}
              rows={this.props.contentPack.entities}
              dataRowFormatter={this._entityRowFormatter}
            />
          </Col>
        </Row>
      </div>
    );
  }
}

export default ContentPackParameters;
