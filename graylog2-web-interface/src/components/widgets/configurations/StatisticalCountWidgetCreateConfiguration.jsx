import PropTypes from 'prop-types';
import React from 'react';
import { Input } from 'components/bootstrap';
import naturalSort from 'javascript-natural-sort';

import { CountWidgetCreateConfiguration } from 'components/widgets/configurations';

import StoreProvider from 'injection/StoreProvider';

const FieldStatisticsStore = StoreProvider.getStore('FieldStatistics');

const sortFields = (fields) => {
  return fields.sort((a, b) => naturalSort(a.toLowerCase(), b.toLowerCase()));
};

const sortStatisticalFunctions = (statisticalFunctions) => {
  return statisticalFunctions.sort();
};

class StatisticalCountWidgetCreateConfiguration extends React.Component {
  static propTypes = {
    config: PropTypes.object.isRequired,
    fields: PropTypes.array.isRequired,
    onChange: PropTypes.func.isRequired,
    setInitialConfiguration: PropTypes.func.isRequired,
  };

  static initialConfiguration = CountWidgetCreateConfiguration.initialConfiguration;

  constructor(props) {
    super(props);

    this.state = {
      sortedFields: sortFields(this.props.fields),
      sortedStatisticalFunctions: sortStatisticalFunctions(FieldStatisticsStore.FUNCTIONS.keySeq().toJS()),
    };

    props.setInitialConfiguration({
      field: this.state.sortedFields[0],
      stats_function: this.state.sortedStatisticalFunctions[0],
    });
  }

  componentWillReceiveProps(nextProps) {
    if (this.props.fields !== nextProps.fields) {
      this.setState({ sortedFields: sortFields(nextProps.fields) });
    }
  }

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

        <CountWidgetCreateConfiguration {...this.props} />
      </fieldset>
    );
  }
}

export default StatisticalCountWidgetCreateConfiguration;
