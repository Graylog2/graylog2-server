import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import lodash from 'lodash';
import Input from 'components/bootstrap/Input';
import { PluginStore } from 'graylog-web-plugin/plugin';

import BootstrapModalForm from 'components/bootstrap/BootstrapModalForm';

import FormsUtils from 'util/FormsUtils';
import ObjectUtils from 'util/ObjectUtils';
import StringUtils from 'util/StringUtils';

/**
 * Widget plugins that want to customize the create modal by adding some inputs need to additionally set the
 * initial configuration for them. This can be achieved in two different ways:
 *
 * ## Setting `initialConfiguration` class property
 * This is preferred, and it should be used every time configuration does not depend on any external state or props.
 * Example:
 * ```
 *    static initialConfiguration = { shouldShowChart: true, description: 'Initial description' };
 * ```
 *
 * ## Calling `setInitialConfiguration` prop
 * This component passes a function called `setInitialConfiguration` to the `configurationCreateComponent` defined
 * for the widget. That function can be called on `constructor` or `componentDidMount` to set the initial configuration
 * values if any of them is derived from state or other props.
 * Note that any configuration key set through this function will have precedence over configuration keys set by
 * `initialConfiguration`.
 * Example:
 * ```
 *    constructor(props) {
 *      super(props);
 *      props.setInitialConfiguration({ field: props.fields[0] });
 *    }
 * ```
 */
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
      fields: [],
      loading: false,
      onModalHidden: () => {},
    };
  },

  getInitialState() {
    const { widgetType } = this.props;
    this.widgetPlugin = this._getWidgetPlugin(widgetType);

    return {
      title: this._getDefaultWidgetTitle(this.widgetPlugin),
      config: this._getInitialConfiguration(this.widgetPlugin),
    };
  },

  componentWillReceiveProps(nextProps) {
    const { widgetType } = this.props;
    if (widgetType !== nextProps.widgetType) {
      this.widgetPlugin = this._getWidgetPlugin(nextProps.widgetType);
      this.setState({ config: this._getInitialConfiguration(this.widgetPlugin) });
    }
  },

  _getWidgetPlugin(widgetType) {
    return PluginStore.exports('widgets').filter(widget => widget.type.toUpperCase() === widgetType.toUpperCase())[0];
  },

  _getInitialConfiguration(widgetPlugin) {
    return lodash.get(widgetPlugin.configurationCreateComponent, 'initialConfiguration', {});
  },

  _setInitialDerivedConfiguration(initialConfig) {
    const { config } = this.state;
    const nextConfig = Object.assign({}, config, initialConfig);
    this.setState({ config: nextConfig });
  },

  open() {
    this.createModal.open();
  },

  hide() {
    this.createModal.close();
  },

  save() {
    const { title, config } = this.state;
    const { onConfigurationSaved } = this.props;
    onConfigurationSaved(title, config);
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
    const { config } = this.state;
    const newConfig = ObjectUtils.clone(config);
    newConfig[key] = value;
    this.setState({ config: newConfig });
  },

  _bindConfigurationValue(event) {
    this._setConfigurationSetting(event.target.name, FormsUtils.getValueFromInput(event.target));
  },

  _onConfigurationValueChange(key, value) {
    // When a single value is passed, we treat it as an event handling
    // When two arguments are given, treat it as a configuration key-value
    if (value !== undefined) {
      this._setConfigurationSetting(key, value);
    } else {
      this._bindConfigurationValue(key);
    }
  },

  _getDefaultWidgetTitle(widgetPlugin) {
    return (widgetPlugin.displayName ? StringUtils.capitalizeFirstLetter(widgetPlugin.displayName) : '');
  },

  render() {
    const { loading, fields, onModalHidden } = this.props;
    const CustomCreateDialog = this.widgetPlugin.configurationCreateComponent;
    const { title, config } = this.state;
    return (
      <BootstrapModalForm ref={(createModal) => { this.createModal = createModal; }}
                          title="Create Dashboard Widget"
                          onModalClose={onModalHidden}
                          onSubmitForm={this.save}
                          submitButtonText={loading ? 'Creating...' : 'Create'}
                          submitButtonDisabled={loading}>
        <fieldset>
          <Input type="text"
                 label="Title"
                 name="title"
                 id="widget-title"
                 required
                 defaultValue={title}
                 onChange={this._bindValue}
                 help="Type a name that describes your widget."
                 autoFocus />
          {CustomCreateDialog && (
            <CustomCreateDialog config={config}
                                fields={fields}
                                onChange={this._onConfigurationValueChange}
                                setInitialConfiguration={this._setInitialDerivedConfiguration} />
          )}
        </fieldset>
      </BootstrapModalForm>
    );
  },
});

export default WidgetCreationModal;
