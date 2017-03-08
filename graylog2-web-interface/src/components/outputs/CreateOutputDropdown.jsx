import React from 'react';
import $ from 'jquery';

import { ConfigurationForm } from 'components/configurationforms';

const CreateOutputDropdown = React.createClass({
  PLACEHOLDER: 'placeholder',
  getInitialState() {
    return {
      typeDefinition: [],
      typeName: this.PLACEHOLDER,
    };
  },
  componentDidMount() {
    this.loadData();
  },
  loadData() {
  },
  render() {
    const outputTypes = $.map(this.props.types, this._formatOutputType);
    return (
      <div>
        <div className="form-inline">
          <select id="input-type" defaultValue={this.PLACEHOLDER} value={this.state.typeName} onChange={this._onTypeChange} className="form-control">
            <option value={this.PLACEHOLDER} disabled>Select Output Type</option>
            {outputTypes}
          </select>
                    &nbsp;
          <button className="btn btn-success" disabled={this.state.typeName === this.PLACEHOLDER} onClick={this._openModal}>Launch new output</button>
        </div>

        <ConfigurationForm ref="configurationForm" key="configuration-form-output" configFields={this.state.typeDefinition} title="Create new Output"
                                   helpBlock={'Select a name of your new output that describes it.'}
                                   typeName={this.state.typeName}
                                   submitAction={this.props.onSubmit} />
      </div>
    );
  },
  _openModal(evt) {
    if (this.state.typeName !== this.PLACEHOLDER && this.state.typeName !== '') {
      this.refs.configurationForm.open();
    }
  },
  _formatOutputType(type, typeName) {
    return (<option key={typeName} value={typeName}>{type.name}</option>);
  },
  _onTypeChange(evt) {
    const outputType = evt.target.value;
    this.setState({ typeName: evt.target.value });
    this.props.getTypeDefinition(outputType, (definition) => {
      this.setState({ typeDefinition: definition.requested_configuration });
    });
  },
});

export default CreateOutputDropdown;
