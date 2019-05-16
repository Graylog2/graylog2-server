import React from 'react';
import PropTypes from 'prop-types';
import lodash from 'lodash';
import { FormGroup, ControlLabel, HelpBlock } from 'react-bootstrap';
import { Input } from 'components/bootstrap';
import { Select } from 'components/common';
import CombinedProvider from 'injection/CombinedProvider';

import SimpleTimerangeSelector from 'components/alert-definitions/common/SimpleTimerangeSelector';

const { StreamsStore } = CombinedProvider.get('Streams');

// TODO: Defaults should come from the backend
const DEFAULTS = {
  batch_size: 500,
  timerange: {
    type: 'relative',
    range: 300,
  },
  streams: [],
};

export default class FilterProcessorParameterForm extends React.Component {
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

  componentDidMount() {
    StreamsStore.load(streams => this.setState({ availableStreams: streams }));
  }

  validate = (parameters) => {
    this.props.onSubmittable(parameters.streams.length > 0 && parameters.batch_size > 0);
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

  handleStreamsChange = (nextStreams) => {
    const selectedStreams = nextStreams === '' ? [] : nextStreams.split(',');
    this.propagateChange('streams', selectedStreams);
  };

  render() {
    const streamOptions = this.state.availableStreams.sort((s1, s2) => s1.title.localeCompare(s2.title))
      .map(stream => ({ label: stream.title, value: stream.id }));

    return (
      <React.Fragment>
        <Input id="timerange-selector"
               label="Timerange"
               help="Select timerange to search.">
          <SimpleTimerangeSelector value={lodash.get(this.props.parameters, 'timerange.range', DEFAULTS.timerange.range)}
                                   onChange={this.handleTimerangeChange} />
        </Input>
        <FormGroup>
          <ControlLabel>Stream</ControlLabel>
          <Select placeholder="Select Streams"
                  onChange={this.handleStreamsChange}
                  options={streamOptions}
                  value={lodash.defaultTo(this.props.parameters.streams, []).join(',')}
                  matchProp={'label'}
                  multi />
          <HelpBlock>
            Select Streams the messages/events should be searched in. Leave it empty to search in all Streams.
          </HelpBlock>
        </FormGroup>
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
