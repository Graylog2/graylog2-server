import React from 'react';
import { Input } from 'components/bootstrap';
import { PluginStore } from 'graylog-web-plugin/plugin';

import BootstrapModalForm from 'components/bootstrap/BootstrapModalForm';

import FormsUtils from 'util/FormsUtils';
import ObjectUtils from 'util/ObjectUtils';
import StringUtils from 'util/StringUtils';

const WidgetCreationModal = React.createClass({
  propTypes: {
    fields: React.PropTypes.array,
    onConfigurationSaved: React.PropTypes.func.isRequired,
    onModalHidden: React.PropTypes.func,
    widgetType: React.PropTypes.string.isRequired,
  },

  getInitialState() {
    this.widgetPlugin = this._getWidgetPlugin(this.props.widgetType);
    return {
      title: this._getDefaultWidgetTitle(this.widgetPlugin),
      config: {},
    };
  },

  componentWillReceiveProps(nextProps) {
    if (this.props.widgetType !== nextProps.widgetType) {
      this.widgetPlugin = this._getWidgetPlugin(nextProps.widgetType);
    }
  },

  _getWidgetPlugin(widgetType) {
    return PluginStore.exports('widgets').filter(widget => widget.type.toUpperCase() === widgetType.toUpperCase())[0];
  },

  _getInitialConfiguration() {
    if (!this.refs.pluginConfiguration) {
      return;
    }

    const configKeys = Object.keys(this.state.config);
    if (configKeys.length === 0) {
      this.setState({ config: this.refs.pluginConfiguration.getInitialConfiguration() });
    }
  },

  open() {
    this.refs.createModal.open();
  },

  hide() {
    this.refs.createModal.close();
  },

  save() {
    this.props.onConfigurationSaved(this.state.title, this.state.config);
  },

  saved() {
    this.setState(this.getInitialState());
    this.hide();
  },

  _setSetting(key, value) {
    const newState = ObjectUtils.clone(this.state);
    newState[key] = value;
    this.setState(newState);
  },

  _bindValue(event) {
    this._setSetting(event.target.name, FormsUtils.getValueFromInput(event.target));
  },

  _setConfigurationSetting(key, value) {
    const newConfig = ObjectUtils.clone(this.state.config);
    newConfig[key] = value;
    this.setState({ config: newConfig });
  },

  _bindConfigurationValue(event) {
    this._setConfigurationSetting(event.target.name, FormsUtils.getValueFromInput(event.target));
  },

  _onConfigurationValueChange() {
    switch (arguments.length) {
      // When a single value is passed, we treat it as an event handling
      case 1:
        this._bindConfigurationValue(arguments[0]);
        break;
      // When two arguments are given, treat it as a configuration key-value
      case 2:
        this._setConfigurationSetting(arguments[0], arguments[1]);
        break;
      default:
        throw new Error('Wrong number of arguments, method only accepts an event or a configuration key-value pair');
    }
  },

  _getDefaultWidgetTitle(widgetPlugin) {
    return (widgetPlugin.displayName ? StringUtils.capitalizeFirstLetter(widgetPlugin.displayName) : '');
  },

  _getSpecificWidgetInputs() {
    if (this.widgetPlugin.configurationCreateComponent) {
      return React.createElement(this.widgetPlugin.configurationCreateComponent, {
        ref: 'pluginConfiguration',
        config: this.state.config,
        fields: this.props.fields,
        onChange: this._onConfigurationValueChange,
      });
    }
  },

  render() {
    return (
      <BootstrapModalForm ref="createModal"
                          title="Create Dashboard Widget"
                          onModalOpen={this._getInitialConfiguration}
                          onModalClose={this.props.onModalHidden}
                          onSubmitForm={this.save}
                          submitButtonText="Create">
        <fieldset>
          <Input type="text"
                 label="Title"
                 name="title"
                 id="widget-title"
                 required
                 defaultValue={this.state.title}
                 onChange={this._bindValue}
                 help="Type a name that describes your widget."
                 autoFocus />
          {this._getSpecificWidgetInputs()}
        </fieldset>
      </BootstrapModalForm>
    );
  },
});

export default WidgetCreationModal;
