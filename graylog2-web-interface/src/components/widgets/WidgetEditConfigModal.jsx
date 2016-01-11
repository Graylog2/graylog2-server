import React from 'react';
import { Input } from 'react-bootstrap';

import DateTime from 'logic/datetimes/DateTime';

import StringUtils from 'util/StringUtils';
import ObjectUtils from 'util/ObjectUtils';
import NumberUtils from 'util/NumberUtils';

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
  _onTitleChange(event) {
    this.setState({description: event.target.value});
  },
  _onCacheTimeChange(event) {
    const numericValue = NumberUtils.isNumber(event.target.value) ? Number(event.target.value) : undefined;
    this.setState({cache_time: numericValue});
  },
  _onConfigurationChange(key, value) {
    const newConfig = ObjectUtils.clone(this.state.config);
    newConfig[key] = value;
    this.setState({config: newConfig});
  },
  _onQueryChange(event) {
    this._onConfigurationChange('query', event.target.value);
  },
  _onConfigurationCheckboxChange(key) {
    return (event) => {
      this._onConfigurationChange(key, event.target.checked);
    };
  },
  _onTimeRangeParamChange(key, value) {
    const newTimeRange = ObjectUtils.clone(this.state.config.timerange);
    newTimeRange[key] = value;
    this._onConfigurationChange('timerange', newTimeRange);
  },
  _onRelativeTimeRangeChange(event) {
    const numericValue = NumberUtils.isNumber(event.target.value) ? Number(event.target.value) : undefined;
    this._onTimeRangeParamChange('range', numericValue);
  },
  _onAbsoluteTimeRangeFromChange(event) {
    const errors = ObjectUtils.clone(this.state.errors);

    try {
      const from = DateTime.parseFromString(event.target.value).toISOString();
      this._onTimeRangeParamChange('from', from);
      errors.from = false;
    } catch (e) {
      errors.from = true;
    }

    this.setState({errors: errors});
  },
  _onAbsoluteTimeRangeToChange(event) {
    const errors = ObjectUtils.clone(this.state.errors);

    try {
      const to = DateTime.parseFromString(event.target.value).toISOString();
      this._onTimeRangeParamChange('to', to);
      errors.to = false;
    } catch (e) {
      errors.to = true;
    }

    this.setState({errors: errors});
  },
  _onKeywordTimeRangeChange(event) {
    this._onTimeRangeParamChange('keyword', event.target.value);
  },
  _onSeriesChange(seriesNo, field) {
    return (event) => {
      const newSeries = ObjectUtils.clone(this.state.config.series);
      newSeries[seriesNo][field] = event.target.value;

      this._onConfigurationChange('series', newSeries);
    };
  },
  _onStatisticalFunctionChange(field) {
    return (event) => {
      this._onConfigurationChange(field, event.target.value);
    };
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
                 label="Search relative time"
                 required
                 min="0"
                 defaultValue={this.state.config.timerange.range}
                 onChange={this._onRelativeTimeRangeChange}
                 help="Number of seconds relative to the moment the search executes. 0 searches in all messages."/>
        );
        break;
      case 'absolute':
        rangeValueInput = (
          <div>
            <Input type="text"
                   label="Search from"
                   required
                   bsStyle={this.state.errors.from === true ? 'error' : null}
                   defaultValue={this._formatDateTime(this.state.config.timerange.from)}
                   onChange={this._onAbsoluteTimeRangeFromChange}
                   help="Earliest time to be included in the search. E.g. 2015-03-27 13:23:41"/>
            <Input type="text"
                   label="Search to"
                   required
                   bsStyle={this.state.errors.to === true ? 'error' : null}
                   defaultValue={this._formatDateTime(this.state.config.timerange.to)}
                   onChange={this._onAbsoluteTimeRangeToChange}
                   help="Latest time to be included in the search. E.g. 2015-03-27 13:23:41"/>
          </div>
        );
        break;
      case 'keyword':
        rangeValueInput = (
          <Input type="text"
                 label="Search keyword"
                 required
                 defaultValue={this.state.config.timerange.keyword}
                 onChange={this._onKeywordTimeRangeChange}
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
               label="Search query"
               defaultValue={this.state.config.query}
               onChange={this._onQueryChange}
               help="Search query that will be executed to get the widget value."/>
      );
    }

    switch (this.state.type.toUpperCase()) {
      case this.props.widgetTypes.STATS_COUNT:
        const defaultStatisticalFunction = this.state.config.stats_function === 'stddev' ? 'std_deviation' : this.state.config.stats_function;
        controls.push(
          <Input key="statsCountStatisticalFunction"
                 type="select"
                 label="Statistical function"
                 defaultValue={defaultStatisticalFunction}
                 onChange={this._onStatisticalFunctionChange('stats_function')}
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
                 label="Display trend"
                 defaultChecked={this.state.config.trend}
                 onChange={this._onConfigurationCheckboxChange('trend')}
                 help="Show trend information for this number."/>
        );

        controls.push(
          <Input key="lowerIsBetter"
                 type="checkbox"
                 label="Lower is better"
                 disabled={this.state.config.trend === false}
                 defaultChecked={this.state.config.lower_is_better}
                 onChange={this._onConfigurationCheckboxChange('lower_is_better')}
                 help="Use green colour when trend goes down."/>
        );
        break;
      case this.props.widgetTypes.QUICKVALUES:
        controls.push(
          <Input key="showPieChart"
                 type="checkbox"
                 label="Show pie chart"
                 defaultChecked={this.state.config.show_pie_chart}
                 onChange={this._onConfigurationCheckboxChange('show_pie_chart')}
                 help="Represent data in a pie chart"/>
        );

        controls.push(
          <Input key="showDataTable"
                 type="checkbox"
                 label="Show data table"
                 defaultChecked={this.state.config.show_data_table}
                 onChange={this._onConfigurationCheckboxChange('show_data_table')}
                 help="Include a table with quantitative information."/>
        );
        break;
      case this.props.widgetTypes.FIELD_CHART:
        controls.push(
          <Input key="fieldChartStatisticalFunction"
                 type="select"
                 label="Statistical function"
                 defaultValue={this.state.config.valuetype}
                 onChange={this._onStatisticalFunctionChange('valuetype')}
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
                     label="Field"
                     defaultValue={series.field}
                     onChange={this._onSeriesChange(seriesNo, 'field')}
                     help="Field used to get the series value."
                     required/>
              <Input type="text"
                     label="Search query"
                     defaultValue={series.query}
                     onChange={this._onSeriesChange(seriesNo, 'query')}
                     help="Search query that will be executed to get the series value."/>
              <Input type="select"
                     label="Statistical function"
                     defaultValue={series.statistical_function}
                     onChange={this._onSeriesChange(seriesNo, 'statistical_function')}
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
                 label="Title"
                 required
                 defaultValue={this.state.description}
                 onChange={this._onTitleChange}
                 help="Type a name that describes your widget."
                 autoFocus />
          <Input type="number"
                 min="1"
                 required
                 label="Cache time"
                 defaultValue={this.state.cache_time}
                 onChange={this._onCacheTimeChange}
                 help="Number of seconds the widget value will be cached."/>
          {this._getTimeRangeFormControls()}
          {this._getSpecificConfigurationControls()}
        </fieldset>
      </BootstrapModalForm>
    );
  },
});

export default WidgetEditConfigModal;
