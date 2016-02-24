import React from 'react';
import { Input } from 'react-bootstrap';
import { PluginStore } from 'graylog-web-plugin/plugin';

import BootstrapModalForm from 'components/bootstrap/BootstrapModalForm';
import Widget from 'components/widgets/Widget';
import GraphVisualization from 'components/visualizations/GraphVisualization';

import FormsUtils from 'util/FormsUtils';
import ObjectUtils from 'util/ObjectUtils';
import StringUtils from 'util/StringUtils';

const WidgetCreationModal = React.createClass({
  propTypes: {
    configuration: React.PropTypes.object,
    fields: React.PropTypes.array,
    isStreamSearch: React.PropTypes.bool,
    onConfigurationSaved: React.PropTypes.func.isRequired,
    onModalHidden: React.PropTypes.func,
    widgetType: React.PropTypes.string.isRequired,
  },

  getInitialState() {
    return {
      title: this._getDefaultWidgetTitle(),
      config: {},
      widgetPlugin: this._getWidgetPlugin(this.props.widgetType),
    };
  },

  componentWillReceiveProps(nextProps) {
    this.setState({widgetPlugin: this._getWidgetPlugin(nextProps.widgetType)});
  },

  widgetPlugins: PluginStore.exports('widgets'),

  _getWidgetPlugin(widgetType) {
    return this.widgetPlugins.filter(widget => widget.type.toUpperCase() === widgetType.toUpperCase())[0];
  },

  _getInitialConfiguration() {
    if (!this.refs.pluginConfiguration) {
      return;
    }

    const configKeys = Object.keys(this.state.config);
    if (configKeys.length === 0) {
      this.setState({config: this.refs.pluginConfiguration.getInitialConfiguration()});
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
    this.setState({config: newConfig});
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

  _getDefaultWidgetTitle() {
    let title = '';

    switch (this.props.widgetType.toUpperCase()) {
      case Widget.Type.SEARCH_RESULT_COUNT:
        title = 'message count';
        break;
      case Widget.Type.STREAM_SEARCH_RESULT_COUNT:
        title = 'stream message count';
        break;
      case Widget.Type.STATS_COUNT:
        title = 'field statistical value';
        break;
      case Widget.Type.QUICKVALUES:
        if (this.props.configuration.field !== undefined) {
          title = this.props.configuration.field + ' quick values';
        } else {
          title = 'field quick values';
        }
        break;
      case Widget.Type.FIELD_CHART:
        if (this.props.configuration.field !== undefined && this.props.configuration.valuetype !== undefined) {
          title = this.props.configuration.field + ' ' + GraphVisualization.getReadableFieldChartStatisticalFunction(this.props.configuration.valuetype) + ' value graph';
        } else {
          title = 'field graph';
        }
        break;
      case Widget.Type.SEARCH_RESULT_CHART:
        if (this.props.isStreamSearch) {
          title = 'stream histogram';
        } else {
          title = 'search histogram';
        }
        break;
      case Widget.Type.STACKED_CHART:
        title = 'combined graph';
        break;
      default:
        throw new Error('Unsupported widget type ' + this.props.widgetType);
    }

    return StringUtils.capitalizeFirstLetter(title);
  },

  _getSpecificWidgetInputs() {
    if (this.state.widgetPlugin.createConfiguration) {
      return React.createElement(this.state.widgetPlugin.createConfiguration, {
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
                 autoFocus/>
          {this._getSpecificWidgetInputs()}
        </fieldset>
      </BootstrapModalForm>
    );
  },
});

export default WidgetCreationModal;
