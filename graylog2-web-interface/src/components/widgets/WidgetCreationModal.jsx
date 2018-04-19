import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import { Input } from 'components/bootstrap';
import { PluginStore } from 'graylog-web-plugin/plugin';

import BootstrapModalForm from 'components/bootstrap/BootstrapModalForm';

import FormsUtils from 'util/FormsUtils';
import ObjectUtils from 'util/ObjectUtils';
import StringUtils from 'util/StringUtils';

const WidgetCreationModal = createReactClass({
  displayName: 'WidgetCreationModal',

  propTypes: {
    fields: PropTypes.array,
    onConfigurationSaved: PropTypes.func.isRequired,
    onModalHidden: PropTypes.func,
    widgetType: PropTypes.string.isRequired,
    loading: PropTypes.bool,
  },

  getDefaultProps() {
    return {
      loading: false,
    };
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
    if (!this.pluginConfiguration) {
      return;
    }

    const configKeys = Object.keys(this.state.config);
    if (configKeys.length === 0) {
      this.setState({ config: this.pluginConfiguration.getInitialConfiguration() });
    }
  },

  open() {
    this.createModal.open();
  },

  hide() {
    this.createModal.close();
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
        ref: (elem) => { this.pluginConfiguration = elem; },
        config: this.state.config,
        fields: this.props.fields,
        onChange: this._onConfigurationValueChange,
      });
    }
  },

  render() {
    const loading = this.props.loading;
    return (
      <BootstrapModalForm ref={(createModal) => { this.createModal = createModal; }}
                          title="Create Dashboard Widget"
                          onModalOpen={this._getInitialConfiguration}
                          onModalClose={this.props.onModalHidden}
                          onSubmitForm={this.save}
                          submitButtonText={loading ? 'Creating...' : 'Create'}
                          submitButtonDisabled={loading}>
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
