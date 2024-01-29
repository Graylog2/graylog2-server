/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import React from 'react';
import PropTypes from 'prop-types';
import camelCase from 'lodash/camelCase';
import cloneDeep from 'lodash/cloneDeep';
import debounce from 'lodash/debounce';
import defaultTo from 'lodash/defaultTo';
import get from 'lodash/get';
import isEmpty from 'lodash/isEmpty';
import merge from 'lodash/merge';
import memoize from 'lodash/memoize';
import max from 'lodash/max';
import union from 'lodash/union';
import moment from 'moment';
import { OrderedMap } from 'immutable';

import Store from 'logic/local-storage/Store';
import { MultiSelect, TimeUnitInput, SearchFiltersFormControls } from 'components/common';
import connect from 'stores/connect';
import Query from 'views/logic/queries/Query';
import Search from 'views/logic/search/Search';
import { extractDurationAndUnit } from 'components/common/TimeUnitInput';
import { Alert, ButtonToolbar, ControlLabel, FormGroup, HelpBlock, Input } from 'components/bootstrap';
import { naturalSortIgnoreCase } from 'util/SortUtils';
import * as FormsUtils from 'util/FormsUtils';
import { isPermitted } from 'util/PermissionsMixin';
import LookupTableParameter from 'views/logic/parameters/LookupTableParameter';
import { LookupTablesActions, LookupTablesStore } from 'stores/lookup-tables/LookupTablesStore';
import generateId from 'logic/generateId';
import parseSearch from 'views/logic/slices/parseSearch';
import withTelemetry from 'logic/telemetry/withTelemetry';
import { getPathnameWithoutId } from 'util/URLUtils';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import withLocation from 'routing/withLocation';

import EditQueryParameterModal from '../event-definition-form/EditQueryParameterModal';
import commonStyles from '../common/commonStyles.css';

export const PLUGGABLE_CONTROLS_HIDDEN_KEY = 'pluggableSearchBarControlsAreHidden';
export const TIME_UNITS = ['HOURS', 'MINUTES', 'SECONDS'];

const LOOKUP_PERMISSIONS = [
  'lookuptables:read',
];

const _buildNewParameter = (name) => ({
  name: name,
  embryonic: true,
  type: 'lut-parameter-v1',
  data_type: 'any',
  title: 'new title',
  // has no binding, no need to set binding property
});

class FilterForm extends React.Component {
  formatStreamIds = memoize(
    (streamIds) => {
      const { streams } = this.props;

      return streamIds
        .map((streamId) => streams.find((s) => s.id === streamId) || streamId)
        .map((streamOrId) => {
          const stream = (typeof streamOrId === 'object' ? streamOrId : { title: streamOrId, id: streamOrId });

          return {
            label: stream.title,
            value: stream.id,
          };
        })
        .sort((s1, s2) => naturalSortIgnoreCase(s1.label, s2.label));
    },
    (streamIds) => streamIds.join('-'),
  );

  _parseQuery = debounce((queryString, searchFilters = new OrderedMap()) => {
    if (!this._userCanViewLookupTables()) {
      return;
    }

    const { queryId, searchTypeId } = this.state;

    const queryBuilder = Query.builder()
      .id(queryId)
      .query({ type: 'elasticsearch', query_string: queryString })
      .timerange({ type: 'relative', range: 1000 })
      .filters(searchFilters.toList())
      .searchTypes([{
        id: searchTypeId,
        type: 'messages',
        limit: 10,
        offset: 0,
      }]);

    const query = queryBuilder.build();

    const search = Search.create().toBuilder()
      .queries([query])
      .build();

    parseSearch(search).then((res) => {
      this._syncParamsWithQuery(res.undeclared);
    });
  }, 250);

  static propTypes = {
    eventDefinition: PropTypes.object.isRequired,
    lookupTables: PropTypes.object.isRequired,
    validation: PropTypes.object.isRequired,
    streams: PropTypes.array.isRequired,
    onChange: PropTypes.func.isRequired,
    currentUser: PropTypes.object.isRequired,
    sendTelemetry: PropTypes.func.isRequired,
    location: PropTypes.object.isRequired,
  };

  constructor(props) {
    super(props);

    const { execute_every_ms: executeEveryMs, search_within_ms: searchWithinMs } = props.eventDefinition.config;
    const searchWithin = extractDurationAndUnit(searchWithinMs, TIME_UNITS);
    const executeEvery = extractDurationAndUnit(executeEveryMs, TIME_UNITS);

    this.state = {
      searchWithinMsDuration: searchWithin.duration,
      searchWithinMsUnit: searchWithin.unit,
      executeEveryMsDuration: executeEvery.duration,
      executeEveryMsUnit: executeEvery.unit,
      queryId: generateId(),
      searchTypeId: generateId(),
      queryParameterStash: {}, // keep already defined parameters around to ease editing
      searchFiltersHidden: false,
    };
  }

  componentDidMount() {
    if (this._userCanViewLookupTables()) {
      LookupTablesActions.searchPaginated(1, 0, undefined, false);
    }
  }

  propagateChange = (key, value) => {
    const { eventDefinition, onChange } = this.props;
    const config = cloneDeep(eventDefinition.config);
    config[key] = value;
    onChange('config', config);
  };

  _syncParamsWithQuery = (paramsInQuery) => {
    const { eventDefinition, onChange } = this.props;
    const config = cloneDeep(eventDefinition.config);
    const queryParameters = config?.query_parameters || [];
    const keptParameters = [];
    const staleParameters = {};

    queryParameters.forEach((p) => {
      if (paramsInQuery.has(p.name)) {
        keptParameters.push(p);
      } else {
        staleParameters[p.name] = p;
      }
    });

    const { queryParameterStash } = this.state;
    const newParameters = [];

    paramsInQuery.forEach((np) => {
      if (!keptParameters.find((p) => p.name === np)) {
        if (queryParameterStash[np]) {
          newParameters.push(queryParameterStash[np]);
        } else {
          newParameters.push(_buildNewParameter(np));
        }
      }
    });

    this.setState({ queryParameterStash: merge(queryParameterStash, staleParameters) });

    config.query_parameters = keptParameters.concat(newParameters);
    onChange('config', config);
  };

  _userCanViewLookupTables = () => {
    const { currentUser } = this.props;

    return isPermitted(currentUser.permissions, LOOKUP_PERMISSIONS);
  };

  handleQueryChange = (event) => {
    this._parseQuery(event.target.value);
    this.handleConfigChange(event);
  };

  handleSearchFiltersChange = (searchFilters) => {
    const { query } = this.props.eventDefinition.config;

    this._parseQuery(query, searchFilters);

    this.propagateChange('filters', searchFilters.toArray());
  };

  hideFiltersPreview = (value) => {
    Store.set(PLUGGABLE_CONTROLS_HIDDEN_KEY, value);
    this.setState({ searchFiltersHidden: value });
  };

  handleConfigChange = (event) => {
    const { name } = event.target;
    const value = FormsUtils.getValueFromInput(event.target);

    if (name === '_is_scheduled') {
      this.props.sendTelemetry(TELEMETRY_EVENT_TYPE.EVENTDEFINITION_CONDITION.FILTER_EXECUTED_AUTOMATICALLY_TOGGLED, {
        app_pathname: getPathnameWithoutId(this.props.location.pathname),
        app_section: 'event-definition-condition',
        app_action_value: 'enable-checkbox',
        is_scheduled: value,
      });
    }

    this.propagateChange(name, value);
  };

  handleStreamsChange = (nextValue) => {
    this.props.sendTelemetry(TELEMETRY_EVENT_TYPE.EVENTDEFINITION_CONDITION.FILTER_STREAM_SELECTED, {
      app_pathname: getPathnameWithoutId(this.props.location.pathname),
      app_section: 'event-definition-condition',
      app_action_value: 'stream-select',
    });

    this.propagateChange('streams', nextValue);
  };

  handleTimeRangeChange = (fieldName) => (nextValue, nextUnit) => {
    const { searchWithinMsUnit, executeEveryMsUnit } = this.state;

    if (fieldName === 'search_within_ms' && nextUnit !== searchWithinMsUnit) {
      this.props.sendTelemetry(TELEMETRY_EVENT_TYPE.EVENTDEFINITION_CONDITION.FILTER_SEARCH_WITHIN_THE_LAST_UNIT_CHANGED, {
        app_pathname: getPathnameWithoutId(this.props.location.pathname),
        app_section: 'event-definition-condition',
        app_action_value: 'searchWithinMsUnit-select',
        new_unit: nextUnit,
      });
    } else if (fieldName === 'execute_every_ms' && nextUnit !== executeEveryMsUnit) {
      this.props.sendTelemetry(TELEMETRY_EVENT_TYPE.EVENTDEFINITION_CONDITION.FILTER_EXECUTE_SEARCH_EVERY_UNIT_CHANGED, {
        app_pathname: getPathnameWithoutId(this.props.location.pathname),
        app_section: 'event-definition-condition',
        app_action_value: 'executeEveryMsUnit-select',
        new_unit: nextUnit,
      });
    }

    const durationInMs = moment.duration(max([nextValue, 1]), nextUnit).asMilliseconds();

    this.propagateChange(fieldName, durationInMs);

    const stateFieldName = camelCase(fieldName);

    this.setState({
      [`${stateFieldName}Duration`]: nextValue,
      [`${stateFieldName}Unit`]: nextUnit,
    });
  };

  renderQueryParameters = () => {
    const { eventDefinition, onChange, lookupTables, validation } = this.props;
    const queryParameters = eventDefinition?.config?.query_parameters || [];

    const onChangeQueryParameters = (newQueryParameters) => {
      const newConfig = { ...eventDefinition.config, query_parameters: newQueryParameters || [] };

      return onChange('config', newConfig);
    };

    if (!this._userCanViewLookupTables()) {
      return (
        <Alert bsStyle="info">
          This account lacks permission to declare Query Parameters from Lookup Tables.
        </Alert>
      );
    }

    const parameterButtons = queryParameters.map((queryParam) => (
      <EditQueryParameterModal key={queryParam.name}
                               queryParameter={LookupTableParameter.fromJSON(queryParam)}
                               embryonic={!!queryParam.embryonic}
                               queryParameters={queryParameters}
                               lookupTables={lookupTables.tables}
                               validation={validation}
                               onChange={onChangeQueryParameters} />
    ));

    if (isEmpty(parameterButtons)) {
      return null;
    }

    const hasEmbryonicParameters = !isEmpty(queryParameters.filter((param) => (param.embryonic)));

    return (
      <FormGroup validationState={validation.errors.query_parameters ? 'error' : null}>
        <ControlLabel>Query Parameters</ControlLabel>
        <Alert bsStyle={hasEmbryonicParameters ? 'danger' : 'info'}>
          <ButtonToolbar>
            {parameterButtons}
          </ButtonToolbar>
        </Alert>
        {hasEmbryonicParameters && (
          <HelpBlock>
            {validation.errors.query_parameters
              ? get(validation, 'errors.query_parameters[0]')
              : 'Please declare missing query parameters by clicking on the buttons above.'}
          </HelpBlock>
        )}
      </FormGroup>
    );
  };

  render() {
    const { eventDefinition, streams, validation } = this.props;
    const { searchWithinMsDuration, searchWithinMsUnit, executeEveryMsDuration, executeEveryMsUnit } = this.state;

    // Ensure deleted streams are still displayed in select
    const allStreamIds = union(streams.map((s) => s.id), defaultTo(eventDefinition.config.streams, []));
    const formattedStreams = this.formatStreamIds(allStreamIds);

    return (
      <fieldset>
        <h2 className={commonStyles.title}>Filter</h2>
        <p>Add information to filter the log messages that are relevant for this Event Definition.</p>
        <Input id="filter-query"
               name="query"
               label="Search Query"
               type="text"
               help={(
                 <span>
                   Search query that Messages should match. You can use the same syntax as in the Search page,
                   including declaring Query Parameters from Lookup Tables by using the <code>$newParameter$</code> syntax.
                 </span>
               )}
               value={defaultTo(eventDefinition.config.query, '')}
               onChange={this.handleQueryChange} />

        {this.renderQueryParameters()}

        {!this.state.searchFiltersHidden && (
          <FormGroup controlId="search-filters">
            <ControlLabel>Search Filters <small className="text-muted">(Optional)</small></ControlLabel>
            <div style={{ maring: '8px 0' }}>
              <SearchFiltersFormControls filters={eventDefinition.config.filters}
                                         onChange={this.handleSearchFiltersChange}
                                         hideFiltersPreview={this.hideFiltersPreview} />
            </div>
          </FormGroup>
        )}

        <FormGroup controlId="filter-streams">
          <ControlLabel>Streams <small className="text-muted">(Optional)</small></ControlLabel>
          <MultiSelect id="filter-streams"
                       matchProp="label"
                       onChange={(selected) => this.handleStreamsChange(selected === '' ? [] : selected.split(','))}
                       options={formattedStreams}
                       value={defaultTo(eventDefinition.config.streams, []).join(',')} />
          <HelpBlock>Select streams the search should include. Searches in all streams if empty.</HelpBlock>
        </FormGroup>

        <FormGroup controlId="search-within" validationState={validation.errors.search_within_ms ? 'error' : null}>
          <TimeUnitInput label="Search within the last"
                         update={this.handleTimeRangeChange('search_within_ms')}
                         value={searchWithinMsDuration}
                         unit={searchWithinMsUnit}
                         units={TIME_UNITS}
                         clearable
                         required />
          {validation.errors.search_within_ms && (
            <HelpBlock>{get(validation, 'errors.search_within_ms[0]')}</HelpBlock>
          )}
        </FormGroup>

        <FormGroup controlId="execute-every" validationState={validation.errors.execute_every_ms ? 'error' : null}>
          <TimeUnitInput label="Execute search every"
                         update={this.handleTimeRangeChange('execute_every_ms')}
                         value={executeEveryMsDuration}
                         unit={executeEveryMsUnit}
                         units={TIME_UNITS}
                         clearable
                         required />
          {validation.errors.execute_every_ms && (
            <HelpBlock>{get(validation, 'errors.execute_every_ms[0]')}</HelpBlock>
          )}
        </FormGroup>
        <Input id="schedule-checkbox"
               type="checkbox"
               name="_is_scheduled"
               label="Enable"
               help="Should this event definition be executed automatically?"
               checked={defaultTo(eventDefinition.config._is_scheduled, true)}
               onChange={this.handleConfigChange} />
      </fieldset>
    );
  }
}

export default connect(withLocation(withTelemetry(FilterForm)), {
  lookupTables: LookupTablesStore,
});
