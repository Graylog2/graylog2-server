import React from 'react';
import PropTypes from 'prop-types';
import lodash from 'lodash';
import { Input } from 'components/bootstrap';

import SimpleTimerangeSelector from 'components/alertdefinitions/common/SimpleTimerangeSelector';

// TODO: Defaults should come from the backend
const DEFAULTS = {
  batch_size: 500,
  timerange: {
    type: 'relative',
    range: 300,
  },
};

export default class CorrelationProcessorParameterForm extends React.Component {
  static propTypes = {
    // eslint-disable-next-line react/no-unused-prop-types
    eventDefinition: PropTypes.object.isRequired,
    parameters: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
    onSubmittable: PropTypes.func.isRequired,
  };

  state = {
    availableStreams: [],
  };

  validate = (parameters) => {
    this.props.onSubmittable(parameters.batch_size > 0);
  };

  propagateChange = (key, value) => {
    const nextParams = Object.assign({}, DEFAULTS, this.props.parameters);
    nextParams[key] = value;
    this.props.onChange(nextParams);
    this.validate(nextParams);
  };

  handleChange = (key) => {
    return e => this.propagateChange(key, e.target.value);
  };

  handleTimerangeChange = (newRange) => {
    this.propagateChange('timerange', {
      type: 'relative',
      range: newRange,
    });
  };

  render() {
    return (
      <React.Fragment>
        <Input id="timerange-selector"
               label="Timerange"
               help="Select timerange to search.">
          <SimpleTimerangeSelector value={lodash.get(this.props.parameters, 'timerange.range', DEFAULTS.timerange.range)}
                                   onChange={this.handleTimerangeChange} />
        </Input>
        <Input id="batch-size"
               name="batch_size"
               label="Batch size"
               type="number"
               help="Maximum number of events that should be returned in a single search."
               value={lodash.defaultTo(this.props.parameters.batch_size, DEFAULTS.batch_size)}
               onChange={this.handleChange('batch_size')} />
      </React.Fragment>
    );
  }
}
