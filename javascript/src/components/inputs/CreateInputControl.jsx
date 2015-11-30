import React from 'react';
import Reflux from 'reflux';
import {Button, Row, Col} from 'react-bootstrap';

import {Select} from 'components/common';
import ConfigurationForm from 'components/configurationforms/ConfigurationForm';

import InputTypesActions from 'actions/inputs/InputTypesActions';
import InputTypesStore from 'stores/inputs/InputTypesStore';
import InputsActions from 'actions/inputs/InputsActions';

const CreateInputControl = React.createClass({
  mixins: [Reflux.connect(InputTypesStore), Reflux.ListenerMethods],
  getInitialState() {
    return {
      selectedInput: undefined,
      selectedInputDefinition: undefined,
    };
  },
  _formatSelectOptions() {
    let options = [];

    if (this.state.inputTypes) {
      const inputTypesIds = Object.keys(this.state.inputTypes);
      options = inputTypesIds.map(id => {
        return {value: id, label: this.state.inputTypes[id]};
      });
      options.sort((inputTypeA, inputTypeB) => inputTypeA.label.toLowerCase().localeCompare(inputTypeB.label.toLowerCase()));
    } else {
      options.push({value: 'none', label: 'No inputs available', disabled: true});
    }

    return options;
  },
  _onInputSelect(selectedInput) {
    this.setState({selectedInput: selectedInput});
    InputTypesActions.get.triggerPromise(selectedInput).then(inputDefinition => this.setState({selectedInputDefinition: inputDefinition}));
  },
  _openModal() {
    this.refs.configurationForm.open();
  },
  _createInput(data) {
    data.global = false;
    InputsActions.create.triggerPromise(data);
  },
  render() {
    let inputModal;
    if (this.state.selectedInputDefinition) {
      const inputTypeName = this.state.inputTypes[this.state.selectedInput];
      inputModal = (
        <ConfigurationForm ref="configurationForm"
                           key="configuration-form-input"
                           configFields={this.state.selectedInputDefinition.requested_configuration}
                           title={<span>Launch new <em>{inputTypeName}</em> input</span>}
                           helpBlock={"Select a name of your new input that describes it."}
                           typeName={this.state.selectedInput}
                           submitAction={this._createInput}/>
      );
    }
    return (
      <Row className="content input-new">
        <Col md={12}>
          <div className="form-inline">
            <div className="form-group" style={{width: 300}}>
              <Select placeholder="Select input" options={this._formatSelectOptions()} matchProp="label" onValueChange={this._onInputSelect}/>
            </div>
            &nbsp;
            <Button bsStyle="success" disabled={!this.state.selectedInput} onClick={this._openModal}>Launch new input</Button>
            <Button href="https://marketplace.graylog.org/" target="_blank" bsStyle="info" style={{marginLeft: 10}}>
              <i className="fa fa-external-link"></i>&nbsp;Find more inputs
            </Button>
          </div>
          {inputModal}
        </Col>
      </Row>
    );
  },
});

export default CreateInputControl;
