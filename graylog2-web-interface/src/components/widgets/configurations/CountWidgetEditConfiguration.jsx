import React, { PropTypes } from 'react';
import { Input } from 'components/bootstrap';

import { QueryConfiguration } from 'components/widgets/configurations';

const CountWidgetEditConfiguration = React.createClass({
  propTypes: {
    config: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
    showQueryConfig: PropTypes.bool,
  },
  getDefaultProps() {
    return {
      showQueryConfig: true,
    };
  },
  render() {
    return (
      <fieldset>
        {this.props.showQueryConfig && <QueryConfiguration {...this.props} />}
        <Input key="trend"
               type="checkbox"
               id="count-trend"
               name="trend"
               label="Display trend"
               defaultChecked={this.props.config.trend}
               onChange={this.props.onChange}
               help="Show trend information for this number." />

        <Input key="lowerIsBetter"
               type="checkbox"
               id="count-lower-is-better"
               name="lower_is_better"
               label="Lower is better"
               disabled={this.props.config.trend === false}
               defaultChecked={this.props.config.lower_is_better}
               onChange={this.props.onChange}
               help="Use green colour when trend goes down." />
      </fieldset>
    );
  },
});

export default CountWidgetEditConfiguration;
