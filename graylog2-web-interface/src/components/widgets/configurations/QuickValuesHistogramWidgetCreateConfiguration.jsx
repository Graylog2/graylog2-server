import PropTypes from 'prop-types';
import React from 'react';
import { Input } from 'components/bootstrap';

const QuickValuesHistogramWidgetCreateConfiguration = React.createClass({
  propTypes: {
    config: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
  },

  getInitialConfiguration() {
    return {
      show_chart_legend: false,
    };
  },

  render() {
    return (
      <fieldset>
        <Input key="show_chart_legend"
               type="checkbox"
               id="show_chart_legend"
               name="show_chart_legend"
               label="Show chart legend"
               checked={this.props.config.show_chart_legend}
               onChange={this.props.onChange}
               help="Show the legend below the chart." />
      </fieldset>
    );
  },
});

export default QuickValuesHistogramWidgetCreateConfiguration;
