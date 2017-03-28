import React from 'react';
import { Input } from 'components/bootstrap';
import { PluginStore } from 'graylog-web-plugin/plugin';

import DateTime from 'logic/datetimes/DateTime';

import StringUtils from 'util/StringUtils';
import ObjectUtils from 'util/ObjectUtils';
import FormsUtils from 'util/FormsUtils';

import BootstrapModalForm from 'components/bootstrap/BootstrapModalForm';

const WidgetEditConfigModal = React.createClass({
  propTypes: {
    onModalHidden: React.PropTypes.func,
    onUpdate: React.PropTypes.func.isRequired,
    widget: React.PropTypes.object.isRequired,
  },

  getInitialState() {
    this.widgetPlugin = this._getWidgetPlugin(this.props.widget.type);
    return {
      description: this.props.widget.description,
      type: this.props.widget.type,
      cache_time: this.props.widget.cache_time,
      config: ObjectUtils.clone(this.props.widget.config), // clone config to not modify it accidentally
      errors: {},
    };
  },

  componentWillReceiveProps(nextProps) {
    this.widgetPlugin = this._getWidgetPlugin(nextProps.widget.type);
  },

  _getWidgetPlugin(widgetType) {
    return PluginStore.exports('widgets').filter(widget => widget.type.toUpperCase() === widgetType.toUpperCase())[0];
  },

  open() {
    this.refs.editModal.open();
  },

  hide() {
    this.refs.editModal.close();
  },

  _getWidgetData() {
    const widget = {};
    const stateKeys = Object.keys(this.state);

    stateKeys.forEach((key) => {
      if (this.state.hasOwnProperty(key) && (key !== 'errors' || key !== 'widgetPlugin')) {
        widget[key] = this.state[key];
      }
    });

    return widget;
  },

  save() {
    const errorKeys = Object.keys(this.state.errors);
    if (!errorKeys.some(key => this.state.errors[key] === true)) {
      this.props.onUpdate(this._getWidgetData());
    }
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

  _setTimeRangeSetting(key, value) {
    const newTimeRange = ObjectUtils.clone(this.state.config.timerange);

    switch (key) {
      case 'from':
      case 'to':
        const errors = ObjectUtils.clone(this.state.errors);

        try {
          newTimeRange[key] = DateTime.parseFromString(value).toISOString();
          errors[key] = false;
        } catch (e) {
          errors[key] = true;
        }

        this.setState({ errors: errors });
        break;
      default:
        newTimeRange[key] = value;
    }

    this._setConfigurationSetting('timerange', newTimeRange);
  },

  _bindTimeRangeValue(event) {
    this._setTimeRangeSetting(event.target.name, FormsUtils.getValueFromInput(event.target));
  },

  _formatDateTime(dateTime) {
    try {
      return DateTime.parseFromString(dateTime).toString();
    } catch (e) {
      return dateTime;
    }
  },

  _getTimeRangeFormControls() {
    const rangeTypeSelector = (
      <Input type="text"
             label="Time range type"
             disabled
             value={StringUtils.capitalizeFirstLetter(this.state.config.timerange.type)}
             help="Type of time range to use in the widget." />
    );

    let rangeValueInput;

    switch (this.state.config.timerange.type) {
      case 'relative':
        rangeValueInput = (
          <Input type="number"
                 id="timerange-relative"
                 name="range"
                 label="Search relative time"
                 required
                 min="0"
                 defaultValue={this.state.config.timerange.range}
                 onChange={this._bindTimeRangeValue}
                 help="Number of seconds relative to the moment the search executes. 0 searches in all messages." />
        );
        break;
      case 'absolute':
        rangeValueInput = (
          <div>
            <Input type="text"
                   id="timerange-absolute-from"
                   name="from"
                   label="Search from"
                   required
                   bsStyle={this.state.errors.from === true ? 'error' : null}
                   defaultValue={this._formatDateTime(this.state.config.timerange.from)}
                   onChange={this._bindTimeRangeValue}
                   help="Earliest time to be included in the search. E.g. 2015-03-27 13:23:41" />
            <Input type="text"
                   id="timerange-absolute-to"
                   name="to"
                   label="Search to"
                   required
                   bsStyle={this.state.errors.to === true ? 'error' : null}
                   defaultValue={this._formatDateTime(this.state.config.timerange.to)}
                   onChange={this._bindTimeRangeValue}
                   help="Latest time to be included in the search. E.g. 2015-03-27 13:23:41" />
          </div>
        );
        break;
      case 'keyword':
        rangeValueInput = (
          <Input type="text"
                 id="timerange-keyword"
                 name="keyword"
                 label="Search keyword"
                 required
                 defaultValue={this.state.config.timerange.keyword}
                 onChange={this._bindTimeRangeValue}
                 help="Search keyword representing the time to be included in the search. E.g. last day" />
        );
        break;
      default:
        rangeValueInput = undefined;
    }

    return (
      <div>
        {rangeTypeSelector}
        {rangeValueInput}
      </div>
    );
  },

  _getSpecificConfigurationControls() {
    if (this.widgetPlugin && this.widgetPlugin.configurationEditComponent) {
      return React.createElement(this.widgetPlugin.configurationEditComponent, {
        id: this.props.widget.id,
        config: this.state.config,
        onChange: this._onConfigurationValueChange,
      });
    }

    return null;
  },
  render() {
    return (
      <BootstrapModalForm ref="editModal"
                          title={`Edit widget "${this.state.description}"`}
                          onSubmitForm={this.save}
                          onModalClose={this.props.onModalHidden}
                          submitButtonText="Update">
        <fieldset>
          <Input type="text"
                 id="title"
                 name="description"
                 label="Title"
                 required
                 defaultValue={this.state.description}
                 onChange={this._bindValue}
                 help="Type a name that describes your widget."
                 autoFocus />
          <Input type="number"
                 min="1"
                 required
                 id="cache_time"
                 name="cache_time"
                 label="Cache time"
                 defaultValue={this.state.cache_time}
                 onChange={this._bindValue}
                 help="Number of seconds the widget value will be cached." />
          {this._getTimeRangeFormControls()}
          {this._getSpecificConfigurationControls()}
        </fieldset>
      </BootstrapModalForm>
    );
  },
});

export default WidgetEditConfigModal;
