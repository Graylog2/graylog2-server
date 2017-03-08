import React, { PropTypes } from 'react';
import { Input } from 'components/bootstrap';

const CountWidgetCreateConfiguration = React.createClass({
  propTypes: {
    config: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
  },

  getInitialConfiguration() {
    return {
      trend: false,
      lower_is_better: false,
    };
  },

  render() {
    return (
      <fieldset>
        <Input key="trend"
               type="checkbox"
               id="count-trend"
               name="trend"
               label="Display trend"
               checked={this.props.config.trend}
               onChange={this.props.onChange}
               help="Show trend information for this number." />

        <Input key="lowerIsBetter"
               type="checkbox"
               id="count-lower-is-better"
               name="lower_is_better"
               label="Lower is better"
               disabled={this.props.config.trend === false}
               checked={this.props.config.lower_is_better}
               onChange={this.props.onChange}
               help="Use green colour when trend goes down." />
      </fieldset>
    );
  },
});

export default CountWidgetCreateConfiguration;
