import React from 'react';
import { ConfigurationForm } from 'components/configurationforms';
import jQuery from 'jquery';

const CreateAlarmCallbackButton = React.createClass({
  propTypes: {
    onCreate: React.PropTypes.func.isRequired,
    types: React.PropTypes.object.isRequired,
  },
  getInitialState() {
    return {
      typeName: this.PLACEHOLDER,
      typeDefinition: {},
    };
  },
  PLACEHOLDER: 'placeholder',
  _openModal() {
    this.refs.configurationForm.open();
  },
  _formatOutputType(typeDefinition, typeName) {
    return (<option key={typeName} value={typeName}>{typeDefinition.name}</option>);
  },
  _onTypeChange(evt) {
    const alarmCallbackType = evt.target.value;
    this.setState({typeName: alarmCallbackType});
    if (this.props.types[alarmCallbackType]) {
      this.setState({typeDefinition: this.props.types[alarmCallbackType].requested_configuration});
    } else {
      this.setState({typeDefinition: {}});
    }
  },
  _handleSubmit(data) {
    this.props.onCreate(data);
    this.setState({typeName: this.PLACEHOLDER});
  },
  _handleCancel() {
    this.setState({typeName: this.PLACEHOLDER});
  },
  render() {
    const alarmCallbackTypes = jQuery.map(this.props.types, this._formatOutputType);
    const humanTypeName = (this.state.typeName && this.props.types[this.state.typeName] ? this.props.types[this.state.typeName].name : 'Alarm Callback');
    const configurationForm = (this.state.typeName !== this.PLACEHOLDER ? <ConfigurationForm ref="configurationForm"
                                                                                           key="configuration-form-output" configFields={this.state.typeDefinition} title={'Create new ' + humanTypeName}
                                                                                           typeName={this.state.typeName} includeTitleField={false}
                                                                                           submitAction={this._handleSubmit} cancelAction={this._handleCancel} /> : null);


    return (
      <div className="form-inline">
        <div className="form-group">
          <select id="input-type" value={this.state.typeName} onChange={this._onTypeChange} className="form-control">
            <option value={this.PLACEHOLDER} disabled>Select Callback Type</option>
            {alarmCallbackTypes}
          </select>
          {' '}
          <button className="btn btn-success form-control" disabled={this.state.typeName === this.PLACEHOLDER}
                  onClick={this._openModal}>Add callback</button>

          &nbsp;
          <a href="https://marketplace.graylog.org/" target="_blank" className="btn btn-info form-control">
            <i className="fa fa-external-link"></i>&nbsp; Find more callbacks
          </a>
        </div>
        {configurationForm}
      </div>
    );
  },
});

export default CreateAlarmCallbackButton;
