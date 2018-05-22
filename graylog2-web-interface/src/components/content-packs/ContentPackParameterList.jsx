import PropTypes from 'prop-types';
import React from 'react';

import { Button, Modal } from 'react-bootstrap';
import BootstrapModalWrapper from 'components/bootstrap/BootstrapModalWrapper';
import DataTable from 'components/common/DataTable';

import ContentPackEditParameter from 'components/content-packs/ContentPackEditParameter';

class ContentPackParameterList extends React.Component {
  static propTypes = {
    contentPack: PropTypes.object.isRequired,
    readOnly: PropTypes.bool,
    onDeleteParameter: PropTypes.func,
    onAddParameter: PropTypes.func,
  };

  static defaultProps = {
    readOnly: false,
    onDeleteParameter: () => {},
    onAddParameter: () => {},
  };

  _parameterRowFormatter = (parameter) => {
    return (
      <tr key={parameter.title}>
        <td>{parameter.title}</td>
        <td>{parameter.name}</td>
        <td>{parameter.description}</td>
        <td>{parameter.type}</td>
        <td>{parameter.default_value}</td>
        {!this.props.readOnly &&
        <td>
          <Button bsStyle="primary" bsSize="small" onClick={() => { this.props.onDeleteParameter(parameter); }}>
            Delete
          </Button>{this._parameterModal(parameter)}
        </td>
        }
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
                                      this.props.onAddParameter(newParameter, parameter);
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
    const headers = this.props.readOnly ?
      ['Title', 'Name', 'Description', 'Value Type', 'Default Value'] :
      ['Title', 'Name', 'Description', 'Value Type', 'Default Value', 'Action'];
    return (
      <div>
        <h2>Parameters list</h2>
        <br />
        { !this.props.readOnly && this._parameterModal() }
        { !this.props.readOnly && (<span><br /><br /></span>) }
        <DataTable
          id="parameter-list"
          headers={headers}
          sortByKey="title"
          noDataText="To use parameters for content packs, at first a parameter must be created and can then be applied to a entity."
          filterKeys={[]}
          rows={this.props.contentPack.parameters}
          dataRowFormatter={this._parameterRowFormatter}
        />
      </div>
    );
  }
}

export default ContentPackParameterList;
