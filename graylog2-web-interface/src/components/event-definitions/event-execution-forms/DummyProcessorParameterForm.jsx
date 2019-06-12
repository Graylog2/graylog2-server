import React from 'react';
import PropTypes from 'prop-types';
import lodash from 'lodash';
import { Input } from 'components/bootstrap';

const DEFAULT_NUMBER_OF_EVENTS = 5;

export default class DummyProcessorParameterForm extends React.Component {
  static propTypes = {
    // eslint-disable-next-line react/no-unused-prop-types
    eventDefinition: PropTypes.object.isRequired,
    parameters: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
    onSubmittable: PropTypes.func.isRequired,
  };

  componentDidMount() {
    // The default values are fine
    this.props.onSubmittable(true);
  }

  validate = (parameters) => {
    this.props.onSubmittable(parameters.number_of_events > 0);
  };

  propagateChange = (key, value) => {
    const nextParams = Object.assign(this.props.parameters);
    nextParams[key] = value;
    this.props.onChange(nextParams);
    this.validate(nextParams);
  };

  handleChange = (key) => {
    return e => this.propagateChange(key, e.target.value);
  };

  render() {
    return (
      <Input id="number-of-events"
             name="number_of_events"
             label="Number of events"
             type="number"
             help="How many dummy events should be created."
             value={lodash.defaultTo(this.props.parameters.number_of_events, DEFAULT_NUMBER_OF_EVENTS)}
             onChange={this.handleChange('number_of_events')} />
    );
  }
}
