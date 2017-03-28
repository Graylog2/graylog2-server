import React, { PropTypes } from 'react';
import { Input } from 'components/bootstrap';

const QuickValuesWidgetCreateConfiguration = React.createClass({
  propTypes: {
    config: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
  },

  getInitialConfiguration() {
    return {
      show_pie_chart: true,
      show_data_table: true,
    };
  },

  render() {
    return (
      <fieldset>
        <Input key="showPieChart"
               type="checkbox"
               id="quickvalues-show-pie-chart"
               name="show_pie_chart"
               label="Show pie chart"
               checked={this.props.config.show_pie_chart}
               onChange={this.props.onChange}
               help="Include a pie chart representation of the data." />

        <Input key="showDataTable"
               type="checkbox"
               id="quickvalues-show-data-table"
               name="show_data_table"
               label="Show data table"
               checked={this.props.config.show_data_table}
               onChange={this.props.onChange}
               help="Include a table with quantitative information." />
      </fieldset>
    );
  },
});

export default QuickValuesWidgetCreateConfiguration;
