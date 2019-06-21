import React from 'react';
import PropTypes from 'prop-types';
import lodash from 'lodash';
import { ControlLabel, FormGroup, HelpBlock } from 'react-bootstrap';
import moment from 'moment';

import { MultiSelect, TimeUnitInput } from 'components/common';
import { Input } from 'components/bootstrap';

import { naturalSortIgnoreCase } from 'util/SortUtils';
import FormsUtils from 'util/FormsUtils';

import commonStyles from '../common/commonStyles.css';

const TIME_UNITS = ['HOURS', 'MINUTES', 'SECONDS'];

const durationFromMs = (durationMs) => {
  const duration = moment.duration(durationMs);
  const timeUnit = TIME_UNITS.find(unit => lodash.isInteger(duration.as(unit))) || lodash.last(TIME_UNITS);
  const durationInUnit = duration.as(timeUnit);
  return {
    timeUnit: timeUnit,
    duration: durationInUnit,
  };
};

class FilterForm extends React.Component {
  static propTypes = {
    eventDefinition: PropTypes.object.isRequired,
    streams: PropTypes.array.isRequired,
    onChange: PropTypes.func.isRequired,
  };

  propagateChange = (key, value) => {
    const { eventDefinition, onChange } = this.props;
    const config = lodash.cloneDeep(eventDefinition.config);
    config[key] = value;
    onChange('config', config);
  };

  handleConfigChange = (event) => {
    const { name } = event.target;
    this.propagateChange(name, FormsUtils.getValueFromInput(event.target));
  };

  handleStreamsChange = (nextValue) => {
    this.propagateChange('streams', nextValue);
  };

  handleTimeRangeChange = (fieldName) => {
    return (nextValue, nextUnit) => {
      const durationInMs = moment.duration(nextValue, nextUnit).asMilliseconds();
      this.propagateChange(fieldName, durationInMs);
    };
  };

  render() {
    const { eventDefinition, streams } = this.props;
    const formattedStreams = streams
      .map(stream => ({ label: stream.title, value: stream.id }))
      .sort((s1, s2) => naturalSortIgnoreCase(s1.label, s2.label));

    const searchWithin = durationFromMs(eventDefinition.config.search_within_ms);
    const executeEvery = durationFromMs(eventDefinition.config.execute_every_ms);

    return (
      <fieldset>
        <h2 className={commonStyles.title}>Filter</h2>
        <Input id="filter-query"
               name="query"
               label="Search Query"
               type="text"
               help="Search query that Messages should match. You can use the same syntax as in the Search page."
               value={lodash.defaultTo(eventDefinition.config.query, '')}
               onChange={this.handleConfigChange} />

        <FormGroup controlId="filter-streams">
          <ControlLabel>Streams <small className="text-muted">(Optional)</small></ControlLabel>
          <MultiSelect id="filter-streams"
                       matchProp="label"
                       onChange={selected => this.handleStreamsChange(selected === '' ? [] : selected.split(','))}
                       options={formattedStreams}
                       value={lodash.defaultTo(eventDefinition.config.streams, []).join(',')} />
          <HelpBlock>Select streams the search should include. Searches in all streams if empty.</HelpBlock>
        </FormGroup>

        <FormGroup controlId="search-within">
          <TimeUnitInput label="Search within the last"
                         update={this.handleTimeRangeChange('search_within_ms')}
                         value={searchWithin.duration}
                         unit={searchWithin.timeUnit}
                         units={TIME_UNITS}
                         required />
        </FormGroup>

        <FormGroup controlId="execute-every">
          <TimeUnitInput label="Execute search every"
                         update={this.handleTimeRangeChange('execute_every_ms')}
                         value={executeEvery.duration}
                         unit={executeEvery.timeUnit}
                         units={TIME_UNITS}
                         required />
        </FormGroup>
      </fieldset>
    );
  }
}

export default FilterForm;
