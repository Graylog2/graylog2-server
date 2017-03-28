import React, { PropTypes } from 'react';
import { Input } from 'components/bootstrap';

import { QueryConfiguration, CountWidgetEditConfiguration } from 'components/widgets/configurations';

import StoreProvider from 'injection/StoreProvider';
const FieldStatisticsStore = StoreProvider.getStore('FieldStatistics');

const StatisticalCountWidgetConfiguration = React.createClass({
  propTypes: {
    config: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
  },
  render() {
    const defaultStatisticalFunction = this.props.config.stats_function === 'stddev' ? 'std_deviation' : this.props.config.stats_function;

    return (
      <fieldset>
        <QueryConfiguration {...this.props} />
        <Input key="statsCountStatisticalFunction"
               type="select"
               id="count-statistical-function"
               name="stats_function"
               label="Statistical function"
               defaultValue={defaultStatisticalFunction}
               onChange={this.props.onChange}
               help="Statistical function applied to the data.">
          {FieldStatisticsStore.FUNCTIONS.keySeq().sort().map((statFunction) => {
            return (
              <option key={statFunction} value={statFunction}>
                {FieldStatisticsStore.FUNCTIONS.get(statFunction)}
              </option>
            );
          })}
        </Input>
        <CountWidgetEditConfiguration {...this.props} showQueryConfig={false} />
      </fieldset>
    );
  },
});

export default StatisticalCountWidgetConfiguration;
