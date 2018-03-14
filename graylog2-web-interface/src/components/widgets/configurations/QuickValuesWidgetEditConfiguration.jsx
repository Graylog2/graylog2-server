import PropTypes from 'prop-types';
import React from 'react';
import { Input } from 'components/bootstrap';

import { QueryConfiguration, QuickValuesConfiguration } from 'components/widgets/configurations';

class QuickValuesWidgetEditConfiguration extends React.Component {
  static propTypes = {
    config: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
  };

  render() {
    return (
      <fieldset>
        <QueryConfiguration {...this.props} />
        <QuickValuesConfiguration {...this.props} />
        <Input key="showPieChart"
               type="checkbox"
               id="quickvalues-show-pie-chart"
               name="show_pie_chart"
               label="Show pie chart"
               defaultChecked={this.props.config.show_pie_chart}
               onChange={this.props.onChange}
               help="Represent data in a pie chart" />

        <Input key="showDataTable"
               type="checkbox"
               id="quickvalues-show-data-table"
               name="show_data_table"
               label="Show data table"
               defaultChecked={this.props.config.show_data_table}
               onChange={this.props.onChange}
               help="Include a table with quantitative information." />
      </fieldset>
    );
  }
}

export default QuickValuesWidgetEditConfiguration;
