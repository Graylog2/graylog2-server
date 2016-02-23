import React from 'react';
import { Input } from 'react-bootstrap';

import DateTime from 'logic/datetimes/DateTime';

import StringUtils from 'util/StringUtils';
import ObjectUtils from 'util/ObjectUtils';
import FormsUtils from 'util/FormsUtils';

import FieldStatisticsStore from 'stores/field-analyzers/FieldStatisticsStore';
import FieldGraphsStore from 'stores/field-analyzers/FieldGraphsStore';
import BootstrapModalForm from 'components/bootstrap/BootstrapModalForm';

const WidgetEditConfigModal = React.createClass({
  propTypes: {
    onModalHidden: React.PropTypes.func,
    onUpdate: React.PropTypes.func.isRequired,
    widget: React.PropTypes.object.isRequired,
    widgetTypes: React.PropTypes.object.isRequired,
  },

  getInitialState() {
    return {
      description: this.props.widget.description,
      type: this.props.widget.type,
      cache_time: this.props.widget.cache_time,
      config: ObjectUtils.clone(this.props.widget.config), // clone config to not modify it accidentally
      errors: {},
    };
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
      if (this.state.hasOwnProperty(key) && key !== 'errors') {
        widget[key] = this.state[key];
      }
    });

    return widget;
  },

  save() {
    const errorKeys = Object.keys(this.state.errors);
    if (!errorKeys.some((key) => this.state.errors[key] === true)) {
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
    this.setState({config: newConfig});
  },

  _bindConfigurationValue(event) {
    this._setConfigurationSetting(event.target.name, FormsUtils.getValueFromInput(event.target));
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

        this.setState({errors: errors});
        break;
      default:
        newTimeRange[key] = value;
    }

    this._setConfigurationSetting('timerange', newTimeRange);
  },

  _bindTimeRangeValue(event) {
    this._setTimeRangeSetting(event.target.name, FormsUtils.getValueFromInput(event.target));
  },

  _setSeriesSetting(seriesNo, key, value, type) {
    const newSeries = ObjectUtils.clone(this.state.config.series);
    newSeries[seriesNo][key] = this._formatSettingValue(value, type);

    this._setConfigurationSetting('series', newSeries);
  },

  _bindSeriesValue(event) {
    this._setSeriesSetting(event.target.getAttribute('data-series'), event.target.name, (event.target.type === 'checkbox' ? event.target.checked : event.target.value), event.target.type);
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
             help="Type of time range to use in the widget."/>
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
                 help="Number of seconds relative to the moment the search executes. 0 searches in all messages."/>
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
                   help="Earliest time to be included in the search. E.g. 2015-03-27 13:23:41"/>
            <Input type="text"
                   id="timerange-absolute-to"
                   name="to"
                   label="Search to"
                   required
                   bsStyle={this.state.errors.to === true ? 'error' : null}
                   defaultValue={this._formatDateTime(this.state.config.timerange.to)}
                   onChange={this._bindTimeRangeValue}
                   help="Latest time to be included in the search. E.g. 2015-03-27 13:23:41"/>
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
                 help="Search keyword representing the time to be included in the search. E.g. last day"/>
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
    const controls = [];

    if (this.state.type !== this.props.widgetTypes.STACKED_CHART) {
      controls.push(
        <Input type="text"
               key="query"
               id="query"
               name="query"
               label="Search query"
               defaultValue={this.state.config.query}
               onChange={this._bindConfigurationValue}
               help="Search query that will be executed to get the widget value."/>
      );
    }

    switch (this.state.type.toUpperCase()) {
      case this.props.widgetTypes.STATS_COUNT:
        const defaultStatisticalFunction = this.state.config.stats_function === 'stddev' ? 'std_deviation' : this.state.config.stats_function;
        controls.push(
          <Input key="statsCountStatisticalFunction"
                 type="select"
                 id="count-statistical-function"
                 name="stats_function"
                 label="Statistical function"
                 defaultValue={defaultStatisticalFunction}
                 onChange={this._bindConfigurationValue}
                 help="Statistical function applied to the data.">
            {FieldStatisticsStore.FUNCTIONS.keySeq().map((statFunction) => {
              return (
                <option key={statFunction} value={statFunction}>
                  {FieldStatisticsStore.FUNCTIONS.get(statFunction)}
                </option>
              );
            })}
          </Input>
        );
      /* falls through */
      case this.props.widgetTypes.SEARCH_RESULT_COUNT:
      case this.props.widgetTypes.STREAM_SEARCH_RESULT_COUNT:
        controls.push(
          <Input key="trend"
                 type="checkbox"
                 id="count-trend"
                 name="trend"
                 label="Display trend"
                 defaultChecked={this.state.config.trend}
                 onChange={this._bindConfigurationValue}
                 help="Show trend information for this number."/>
        );

        controls.push(
          <Input key="lowerIsBetter"
                 type="checkbox"
                 id="count-lower-is-better"
                 name="lower_is_better"
                 label="Lower is better"
                 disabled={this.state.config.trend === false}
                 defaultChecked={this.state.config.lower_is_better}
                 onChange={this._bindConfigurationValue}
                 help="Use green colour when trend goes down."/>
        );
        break;
      case this.props.widgetTypes.QUICKVALUES:
        controls.push(
          <Input key="showPieChart"
                 type="checkbox"
                 id="quickvalues-show-pie-chart"
                 name="show_pie_chart"
                 label="Show pie chart"
                 defaultChecked={this.state.config.show_pie_chart}
                 onChange={this._bindConfigurationValue}
                 help="Represent data in a pie chart"/>
        );

        controls.push(
          <Input key="showDataTable"
                 type="checkbox"
                 id="quickvalues-show-data-table"
                 name="show_data_table"
                 label="Show data table"
                 defaultChecked={this.state.config.show_data_table}
                 onChange={this._bindConfigurationValue}
                 help="Include a table with quantitative information."/>
        );
        break;
      case this.props.widgetTypes.FIELD_CHART:
        controls.push(
          <Input key="fieldChartStatisticalFunction"
                 id="chart-statistical-function"
                 name="valuetype"
                 type="select"
                 label="Statistical function"
                 defaultValue={this.state.config.valuetype}
                 onChange={this._bindConfigurationValue}
                 help="Statistical function applied to the data.">
            {FieldGraphsStore.constructor.FUNCTIONS.keySeq().map((statFunction) => {
              return (
                <option key={statFunction} value={statFunction}>
                  {FieldGraphsStore.constructor.FUNCTIONS.get(statFunction)}
                </option>
              );
            })}
          </Input>
        );
        break;
      case this.props.widgetTypes.STACKED_CHART:
        this.state.config.series.forEach((series) => {
          const seriesNo = this.state.config.series.indexOf(series);
          controls.push(
            <fieldset key={'series' + seriesNo}>
              <legend>Series #{seriesNo + 1}</legend>
              <Input type="text"
                     id={`series-${seriesNo}-field`}
                     name="field"
                     label="Field"
                     data-series={seriesNo}
                     defaultValue={series.field}
                     onChange={this._bindSeriesValue}
                     help="Field used to get the series value."
                     required/>
              <Input type="text"
                     id={`series-${seriesNo}-query`}
                     name="query"
                     label="Search query"
                     data-series={seriesNo}
                     defaultValue={series.query}
                     onChange={this._bindSeriesValue}
                     help="Search query that will be executed to get the series value."/>
              <Input type="select"
                     id={`series-${seriesNo}-statistical-function`}
                     name="statistical_function"
                     label="Statistical function"
                     data-series={seriesNo}
                     defaultValue={series.statistical_function}
                     onChange={this._bindSeriesValue}
                     help="Statistical function applied to the series.">
                {FieldGraphsStore.constructor.FUNCTIONS.keySeq().map((statFunction) => {
                  return (
                    <option key={statFunction} value={statFunction}>
                      {FieldGraphsStore.constructor.FUNCTIONS.get(statFunction)}
                    </option>
                  );
                })}
              </Input>
            </fieldset>
          );
        }, this);
        break;
      default:
    }

    return controls;
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
                 autoFocus/>
          <Input type="number"
                 min="1"
                 required
                 id="cache_time"
                 name="cache_time"
                 label="Cache time"
                 defaultValue={this.state.cache_time}
                 onChange={this._bindValue}
                 help="Number of seconds the widget value will be cached."/>
          {this._getTimeRangeFormControls()}
          {this._getSpecificConfigurationControls()}
        </fieldset>
      </BootstrapModalForm>
    );
  },
});

export default WidgetEditConfigModal;
