import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import styled from 'styled-components';

import { Col, Row, Button } from 'components/graylog';
import { ExternalLinkButton, Select } from 'components/common';

import ActionsProvider from 'injection/ActionsProvider';

import StoreProvider from 'injection/StoreProvider';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { InputForm } from 'components/inputs';

const InputTypesActions = ActionsProvider.getActions('InputTypes');
const InputsActions = ActionsProvider.getActions('Inputs');
const InputTypesStore = StoreProvider.getStore('InputTypes');

const NewInputRow = styled(Row)`
  margin-bottom: 8px;
`;

const CreateInputControl = createReactClass({
  displayName: 'CreateInputControl',
  mixins: [Reflux.connect(InputTypesStore)],

  getInitialState() {
    return {
      selectedInput: undefined,
      selectedInputDefinition: undefined,
    };
  },

  _formatSelectOptions() {
    let options = [];

    const { inputTypes } = this.state;
    if (inputTypes) {
      const inputTypesIds = Object.keys(inputTypes);
      options = inputTypesIds.map((id) => {
        return { value: id, label: inputTypes[id] };
      });
      options.sort((inputTypeA, inputTypeB) => inputTypeA.label.toLowerCase().localeCompare(inputTypeB.label.toLowerCase()));
    } else {
      options.push({ value: 'none', label: 'No inputs available', disabled: true });
    }

    return options;
  },

  _onInputSelect(selectedInput) {
    if (selectedInput === '') {
      this.setState(this.getInitialState());
    }

    this.setState({ selectedInput: selectedInput });
    InputTypesActions.get.triggerPromise(selectedInput).then((inputDefinition) => this.setState({ selectedInputDefinition: inputDefinition }));
  },

  _openModal(event) {
    event.preventDefault();
    const { selectedInput } = this.state;

    const customConfiguration = PluginStore.exports('inputConfiguration')
      .find((inputConfig) => inputConfig.type === selectedInput);

    if (customConfiguration) {
      const onClose = () => this.setState({ customInputsComponent: undefined });
      const CustomInputsConfiguration = customConfiguration.component;

      this.setState({
        customInputsComponent: <CustomInputsConfiguration onClose={onClose} />,
      });
    }

    this.configurationForm.open();
  },

  _createInput(data) {
    InputsActions.create(data).then(() => {
      this.setState(this.getInitialState());
    });
  },

  render() {
    let inputModal;
    const { selectedInputDefinition, selectedInput, inputTypes, customInputsComponent } = this.state;

    if (selectedInputDefinition && !customInputsComponent) {
      const inputTypeName = inputTypes[selectedInput];
      inputModal = (
        <InputForm ref={(configurationForm) => { this.configurationForm = configurationForm; }}
                   key="configuration-form-input"
                   configFields={selectedInputDefinition.requested_configuration}
                   title={<span>Launch new <em>{inputTypeName}</em> input</span>}
                   helpBlock="Select a name of your new input that describes it."
                   typeName={selectedInput}
                   submitAction={this._createInput} />
      );
    }

    return (
      <NewInputRow className="content">
        <Col md={12}>
          <form className="form-inline" onSubmit={this._openModal}>
            <div className="form-group" style={{ width: 300 }}>
              <Select placeholder="Select input"
                      options={this._formatSelectOptions()}
                      matchProp="label"
                      onChange={this._onInputSelect}
                      value={selectedInput} />
            </div>
            &nbsp;
            <Button bsStyle="success" type="submit" disabled={!selectedInput}>Launch new input</Button>
            <ExternalLinkButton href="https://marketplace.graylog.org/"
                                bsStyle="info"
                                style={{ marginLeft: 10 }}>
              Find more inputs
            </ExternalLinkButton>
          </form>
          {inputModal || customInputsComponent}
        </Col>
      </NewInputRow>
    );
  },
});

export default CreateInputControl;
