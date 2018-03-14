import PropTypes from 'prop-types';
import React from 'react';
import { Input } from 'components/bootstrap';

import { QueryConfiguration } from 'components/widgets/configurations';
import StoreProvider from 'injection/StoreProvider';
const FieldGraphsStore = StoreProvider.getStore('FieldGraphs');

class FieldChartWidgetConfiguration extends React.Component {
  static propTypes = {
    config: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
  };

  render() {
    return (
      <fieldset>
        <QueryConfiguration {...this.props} />
        <Input key="fieldChartStatisticalFunction"
               id="chart-statistical-function"
               name="valuetype"
               type="select"
               label="Statistical function"
               defaultValue={this.props.config.valuetype}
               onChange={this.props.onChange}
               help="Statistical function applied to the data.">
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
  }
}

export default FieldChartWidgetConfiguration;
