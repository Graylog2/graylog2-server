import PropTypes from 'prop-types';
import React from 'react';

import { Button, Modal, ButtonToolbar } from 'react-bootstrap';
import BootstrapModalWrapper from 'components/bootstrap/BootstrapModalWrapper';
import { DataTable, SearchForm } from 'components/common';

import ObjectUtils from 'util/ObjectUtils';
import ContentPackEditParameter from 'components/content-packs/ContentPackEditParameter';

import ContentPackParameterListStyle from './ContentPackParameterList.css';
import ContentPackUtils from './ContentPackUtils';

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

  constructor(props) {
    super(props);

    this.state = {
      filteredParameters: props.contentPack.parameters,
      filter: undefined,
    };
  }

  componentWillReceiveProps(newProps) {
    this._filterParameters(this.state.filter, newProps.contentPack.parameters);
  }

  _parameterRowFormatter = (parameter) => {
    return (
      <tr key={parameter.title}>
        <td>{parameter.title}</td>
        <td>{parameter.name}</td>
        <td>{parameter.description}</td>
        <td>{parameter.type}</td>
        <td>{ContentPackUtils.convertToString(parameter)}</td>
        {!this.props.readOnly &&
        <td>
          <ButtonToolbar>
            <Button bsStyle="primary" bsSize="small" onClick={() => { this.props.onDeleteParameter(parameter); }}>
              Delete
            </Button>{this._parameterModal(parameter)}
          </ButtonToolbar>
        </td>
        }
      </tr>
    );
  };

  _parameterModal(parameter) {
    let modalRef;
    let editParameter;

    const closeModal = () => {
      modalRef.close();
    };

    const open = () => {
      modalRef.open();
    };

    const addParameter = () => {
      editParameter.addNewParameter();
    };

    const size = parameter ? 'small' : 'small';
    const name = parameter ? 'Edit' : 'Create parameter';

    const modal = (
      <BootstrapModalWrapper ref={(node) => { modalRef = node; }} bsSize="large">
        <Modal.Header closeButton>
          <Modal.Title>Parameter</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <ContentPackEditParameter ref={(node) => { editParameter = node; }}
                                    parameters={this.props.contentPack.parameters}
                                    onUpdateParameter={(newParameter) => {
                                      this.props.onAddParameter(newParameter, parameter);
                                      closeModal();
                                    }}
                                    parameterToEdit={parameter} />
        </Modal.Body>
        <Modal.Footer>
          <div className="pull-right">
            <ButtonToolbar>
              <Button bsStyle="primary" onClick={addParameter}>Save</Button>
              <Button onClick={closeModal}>Close</Button>
            </ButtonToolbar>
          </div>
        </Modal.Footer>
      </BootstrapModalWrapper>
    );

    return (
      <Button bsStyle="info"
              bsSize={size}
              onClick={() => { open(); }}>
        {name}
        {modal}
      </Button>
    );
  }

  _filterParameters = (filter, parametersArg) => {
    const parameters = ObjectUtils.clone(parametersArg || this.props.contentPack.parameters);
    if (!filter || filter.length <= 0) {
      this.setState({ filteredParameters: parameters, filter: undefined });
      return;
    }
    const regexp = RegExp(filter, 'i');
    const filteredParameters = parameters.filter((parameter) => {
      return regexp.test(parameter.title) || regexp.test(parameter.description) || regexp.test(parameter.name);
    });

    this.setState({ filteredParameters: filteredParameters, filter: filter });
  };

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
        <SearchForm
          onSearch={this._filterParameters}
          onReset={() => { this._filterParameters(''); }}
          searchButtonLabel='Filter'
        />
        <DataTable
          id="parameter-list"
          headers={headers}
          className={ContentPackParameterListStyle.scrollable}
          sortByKey="title"
          noDataText="To use parameters for content packs, at first a parameter must be created and can then be applied to a entity."
          filterKeys={[]}
          rows={this.state.filteredParameters}
          dataRowFormatter={this._parameterRowFormatter}
        />
      </div>
    );
  }
}

export default ContentPackParameterList;
