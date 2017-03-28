import React, { PropTypes } from 'react';
import { Input } from 'components/bootstrap';
import naturalSort from 'javascript-natural-sort';

import { CountWidgetCreateConfiguration } from 'components/widgets/configurations';

import StoreProvider from 'injection/StoreProvider';
const FieldStatisticsStore = StoreProvider.getStore('FieldStatistics');

const StatisticalCountWidgetCreateConfiguration = React.createClass({
  propTypes: {
    config: PropTypes.object.isRequired,
    fields: PropTypes.array.isRequired,
    onChange: PropTypes.func.isRequired,
  },

  getInitialState() {
    return {
      sortedFields: this._sortFields(this.props.fields),
      sortedStatisticalFunctions: this._sortStatisticalFunctions(FieldStatisticsStore.FUNCTIONS.keySeq().toJS()),
    };
  },

  componentWillReceiveProps(nextProps) {
    if (this.props.fields !== nextProps.fields) {
      this.setState({ sortedFields: this._sortFields(nextProps.fields) });
    }
  },

  _sortFields(fields) {
    return fields.sort((a, b) => naturalSort(a.toLowerCase(), b.toLowerCase()));
  },

  _sortStatisticalFunctions(statisticalFunctions) {
    return statisticalFunctions.sort();
  },

  getInitialConfiguration() {
    const countConfiguration = this.refs.countConfiguration.getInitialConfiguration();
    const initialConfiguration = {};

    Object.keys(countConfiguration).forEach(key => initialConfiguration[key] = countConfiguration[key]);
    initialConfiguration.field = this.state.sortedFields[0];
    initialConfiguration.stats_function = this.state.sortedStatisticalFunctions[0];

    return initialConfiguration;
  },

  render() {
    return (
      <fieldset>
        <Input key="field"
               type="select"
               id="stats-count-field"
               name="field"
               label="Field name"
               help="Select the field name you want to use in the widget."
               value={this.props.config.field}
               onChange={this.props.onChange}>
          {this.state.sortedFields.map((field) => {
            return <option key={field} value={field}>{field}</option>;
          })
          }
        </Input>

        <Input key="stats_function"
               type="select"
               id="stats-count-function"
               name="stats_function"
               label="Statistical function"
               help="Select the statistical function to use in the widget."
               value={this.props.config.stats_function}
               onChange={this.props.onChange}>
          {this.state.sortedStatisticalFunctions.map((statFunction) => {
            return (
              <option key={statFunction} value={statFunction}>
                {FieldStatisticsStore.FUNCTIONS.get(statFunction)}
              </option>
            );
          })}
        </Input>

        <CountWidgetCreateConfiguration ref="countConfiguration" {...this.props} />
      </fieldset>
    );
  },
});

export default StatisticalCountWidgetCreateConfiguration;
