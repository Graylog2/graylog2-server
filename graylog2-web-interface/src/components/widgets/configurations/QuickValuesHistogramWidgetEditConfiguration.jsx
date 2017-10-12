import PropTypes from 'prop-types';
import React from 'react';
import { Input } from 'components/bootstrap';

import { QueryConfiguration, QuickValuesConfiguration } from 'components/widgets/configurations';

const QuickValuesHistogramWidgetEditConfiguration = React.createClass({
  propTypes: {
    config: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
  },

  INTERVALS: ['year', 'quarter', 'month', 'week', 'day', 'hour', 'minute'],

  render() {
    return (
      <fieldset>
        <QueryConfiguration {...this.props} />
        <QuickValuesConfiguration isHistogram {...this.props} />
        <Input type="select"
               name="interval"
               label="Interval"
               defaultValue={this.props.config.interval}
               onChange={this.props.onChange}
               help="The interval for the buckets in the histogram.">
          {this.INTERVALS.map((interval) => {
            return (
              <option key={interval} value={interval}>
                {interval}
              </option>
            );
          })}
        </Input>
      </fieldset>
    );
  },
});

export default QuickValuesHistogramWidgetEditConfiguration;
