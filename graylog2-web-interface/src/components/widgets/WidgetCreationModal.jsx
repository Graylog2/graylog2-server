import React from 'react';
import { Input } from 'react-bootstrap';
import Immutable from 'immutable';

import BootstrapModalForm from 'components/bootstrap/BootstrapModalForm';
import Widget from 'components/widgets/Widget';
import GraphVisualization from 'components/visualizations/GraphVisualization';

import FieldStatisticsStore from 'stores/field-analyzers/FieldStatisticsStore';

import StringUtils from 'util/StringUtils';

class Fieldset extends React.Component {
  static propTypes = {
    children: React.PropTypes.any,
  };

  render() {
    return (
      <fieldset>
        {this.props.children}
      </fieldset>
    );
  }
}

const WidgetCreationModal = React.createClass({
  propTypes: {
    configuration: React.PropTypes.object,
    fields: React.PropTypes.array,
    isStreamSearch: React.PropTypes.bool,
    onConfigurationSaved: React.PropTypes.func.isRequired,
    onModalHidden: React.PropTypes.func,
    supportsTrending: React.PropTypes.bool,
    widgetType: React.PropTypes.string.isRequired,
  },
  getInitialState() {
    this.initialConfiguration = Immutable.Map();
    return {
      title: '',
      configuration: this.initialConfiguration,
    };
  },
  getEffectiveConfiguration() {
    return this.initialConfiguration.merge(Immutable.Map(this.state.configuration));
  },
  open() {
    this.refs.createModal.open();
  },
  hide() {
    this.refs.createModal.close();
  },
  save() {
    const configuration = this.getEffectiveConfiguration();
    this.props.onConfigurationSaved(this._getCurrentTitle(), configuration);
  },
  saved() {
    this.setState(this.getInitialState());
    this.hide();
  },
  _getCurrentTitle() {
    return this.state.title || this._getDefaultWidgetTitle();
  },
  // We need to set the default configuration in case some inputs are never changed
  _setInitialConfiguration() {
    let initialConfiguration = this.initialConfiguration;
    if (this.refs.inputFieldset !== undefined) {
      const fieldsetChildren = this.refs.inputFieldset.props.children;
      React.Children.forEach(fieldsetChildren, (child) => {
        // We only care about children with refs, as all inputs have one
        if (child.ref !== undefined && child.ref !== 'title') {
          const input = this.refs[child.ref];
          let value;
          if (input.props.type === 'checkbox') {
            value = input.getChecked();
          } else {
            value = input.getValue();
          }

          if (value !== undefined) {
            initialConfiguration = initialConfiguration.set(child.ref, value);
          }
        }
      }, this);
    }
    this.initialConfiguration = initialConfiguration;
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
    const controls = [];

    switch (this.props.widgetType) {
      case Widget.Type.SEARCH_RESULT_COUNT:
      case Widget.Type.STREAM_SEARCH_RESULT_COUNT:
      case Widget.Type.STATS_COUNT:
        if (this.props.widgetType === Widget.Type.STATS_COUNT) {
          controls.push(
            <Input key="field"
                   type="select"
                   ref="field"
                   label="Field name"
                   help="Select the field name you want to use in the widget."
                   onChange={() => this._onConfigurationInputChange('field')}>
              {this.props.fields
                .sort((a, b) => a.toLowerCase().localeCompare(b.toLowerCase()))
                .map((field) => {
                  return <option key={field} value={field}>{field}</option>;
                })
                }
            </Input>
          );
          controls.push(
            <Input key="stats_function"
                   type="select"
                   ref="stats_function"
                   label="Statistical function"
                   help="Select the statistical function to use in the widget."
                   onChange={() => this._onConfigurationInputChange('stats_function')}>
              {FieldStatisticsStore.FUNCTIONS.keySeq().map((statFunction) => {
                return (
                <option key={statFunction} value={statFunction}>
                  {FieldStatisticsStore.FUNCTIONS.get(statFunction)}
                </option>
                  );
              })}
            </Input>
          );
        }

        if (this.props.supportsTrending) {
          controls.push(
            <Input key="trend"
                   type="checkbox"
                   ref="trend"
                   label="Display trend"
                   defaultChecked={false}
                   onChange={() => this._onConfigurationCheckboxChange('trend')}
                   help="Show trend information for this number."/>
          );
          controls.push(
            <Input key="lowerIsBetter"
                   type="checkbox"
                   ref="lower_is_better"
                   label="Lower is better"
                   disabled={!this.state.configuration.get('trend', false)}
                   defaultChecked={false}
                   onChange={() => this._onConfigurationCheckboxChange('lower_is_better')}
                   help="Use green colour when trend goes down."/>
          );
        }
        break;
      case Widget.Type.QUICKVALUES:
        controls.push(
          <Input key="showPieChart"
                 type="checkbox"
                 ref="show_pie_chart"
                 label="Show pie chart"
                 defaultChecked={false}
                 onChange={() => this._onConfigurationCheckboxChange('show_pie_chart')}
                 help="Include a pie chart representation of the data."/>
        );
        controls.push(
          <Input key="showDataTable"
                 type="checkbox"
                 ref="show_data_table"
                 label="Show data table"
                 defaultChecked
                 onChange={() => this._onConfigurationCheckboxChange('show_data_table')}
                 help="Include a table with quantitative information."/>
        );
        break;
    }

    return controls;
  },
  _onTitleChange() {
    this.setState({title: this.refs.title.getValue()});
  },
  _onConfigurationChange(key, value) {
    const newConfiguration = this.state.configuration.set(key, value);
    this.setState({configuration: newConfiguration});
  },
  _onConfigurationCheckboxChange(ref) {
    this._onConfigurationChange(ref, this.refs[ref].getChecked());
  },
  _onConfigurationInputChange(ref) {
    this._onConfigurationChange(ref, this.refs[ref].getValue());
  },
  render() {
    return (
      <BootstrapModalForm ref="createModal"
                          title="Create Dashboard Widget"
                          onModalOpen={this._setInitialConfiguration}
                          onModalClose={this.props.onModalHidden}
                          onSubmitForm={this.save}
                          submitButtonText="Create">
        <Fieldset ref="inputFieldset">
          <Input type="text"
                 label="Title"
                 ref="title"
                 required
                 defaultValue={this._getCurrentTitle()}
                 onChange={this._onTitleChange}
                 help="Type a name that describes your widget."
                 autoFocus />
          {this._getSpecificWidgetInputs()}
        </Fieldset>
      </BootstrapModalForm>
    );
  },
});

export default WidgetCreationModal;
