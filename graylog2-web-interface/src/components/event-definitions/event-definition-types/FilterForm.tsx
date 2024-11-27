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

import * as React from 'react';
import { useCallback, useContext, useEffect, useState } from 'react';
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
import type { $PropertyType } from 'utility-types';

import { describeExpression } from 'util/CronUtils';
import { getPathnameWithoutId } from 'util/URLUtils';
import { isPermitted } from 'util/PermissionsMixin';
import * as FormsUtils from 'util/FormsUtils';
import { naturalSortIgnoreCase } from 'util/SortUtils';
import FormWarningsContext from 'contexts/FormWarningsContext';
import { useStore } from 'stores/connect';
import Store from 'logic/local-storage/Store';
import { MultiSelect, TimeUnitInput, SearchFiltersFormControls, TimezoneSelect } from 'components/common';
import Query from 'views/logic/queries/Query';
import type { RelativeTimeRangeWithEnd, ElasticsearchQueryString } from 'views/logic/queries/Query';
import Search from 'views/logic/search/Search';
import { extractDurationAndUnit } from 'components/common/TimeUnitInput';
import { Alert, ButtonToolbar, ControlLabel, FormGroup, HelpBlock, Input } from 'components/bootstrap';
import RelativeTime from 'components/common/RelativeTime';
import type { LookupTableParameterJson } from 'views/logic/parameters/LookupTableParameter';
import LookupTableParameter from 'views/logic/parameters/LookupTableParameter';
import { LookupTablesActions, LookupTablesStore } from 'stores/lookup-tables/LookupTablesStore';
import validateQuery from 'views/components/searchbar/queryvalidation/validateQuery';
import generateId from 'logic/generateId';
import parseSearch from 'views/logic/slices/parseSearch';
import useLocation from 'routing/useLocation';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import type User from 'logic/users/User';
import useUserDateTime from 'hooks/useUserDateTime';
import type { EventDefinition } from 'components/event-definitions/event-definitions-types';
import type { Stream } from 'views/stores/StreamsStore';
import type { QueryValidationState } from 'views/components/searchbar/queryvalidation/types';
import { indicesInWarmTier, isSearchingWarmTier } from 'views/components/searchbar/queryvalidation/warmTierValidation';
import type { FiltersType } from 'views/types';
import { defaultCompare } from 'logic/DefaultCompare';
import type { EventDefinitionValidation } from 'components/event-definitions/types';

import EditQueryParameterModal from '../event-definition-form/EditQueryParameterModal';
import commonStyles from '../common/commonStyles.css';

export const PLUGGABLE_CONTROLS_HIDDEN_KEY = 'pluggableSearchBarControlsAreHidden';
export const TIME_UNITS = ['HOURS', 'MINUTES', 'SECONDS'];
type LookupTableParameterJsonEmbryonic = Partial<LookupTableParameterJson> & {
  embryonic?: boolean,
}
const LOOKUP_PERMISSIONS = [
  'lookuptables:read',
];

const buildNewParameter = (name: string): LookupTableParameterJsonEmbryonic => ({
  name: name,
  embryonic: true,
  type: 'lut-parameter-v1',
  data_type: 'any',
  title: 'new title',
});

type EventDefinitionConfig = $PropertyType<EventDefinition, 'config'>;

type Props = {
  currentUser: User,
  eventDefinition: EventDefinition,
  onChange: (name: string, config: EventDefinitionConfig) => void,
  streams: Array<Stream>,
  validation: EventDefinitionValidation
}

const FilterForm = ({
  currentUser,
  eventDefinition,
  onChange,
  streams,
  validation,
} : Props) => {
  const { execute_every_ms: executeEveryMs, search_within_ms: searchWithinMs } = eventDefinition.config;
  const [currentConfig, setCurrentConfig] = useState(eventDefinition.config);
  const searchWithin = extractDurationAndUnit(searchWithinMs, TIME_UNITS);
  const executeEvery = extractDurationAndUnit(executeEveryMs, TIME_UNITS);
  const { userTimezone } = useUserDateTime();
  const { setFieldWarning, warnings } = useContext(FormWarningsContext);
  const validationState = (warnings?.queryString) as QueryValidationState;
  const warmTierRanges = indicesInWarmTier(validationState);

  const { tables } = useStore(LookupTablesStore);
  const { pathname } = useLocation();

  const sendTelemetry = useSendTelemetry();

  const queryId = generateId();
  const searchTypeId = generateId();

  const [queryParameterStash, setQueryParameterStash] = useState<object>({});
  const [searchFiltersHidden, setSearchFiltersHidden] = useState<boolean>(false);
  const [searchWithinMsUnit, setSearchWithinMsUnit] = useState<string>(searchWithin.unit);
  const [executeEveryMsUnit, setExecuteEveryMsUnit] = useState<string>(executeEvery.unit);
  const [searchWithinMsDuration, setSearchWithinMsDuration] = useState<number>(searchWithin.duration);
  const [executeEveryMsDuration, setExecuteEveryMsDuration] = useState<number>(executeEvery.duration);

  const userCanViewLookupTables = useCallback(
    () => isPermitted(currentUser.permissions, LOOKUP_PERMISSIONS),
    [currentUser.permissions],
  );

  const streamCategoryOptions = React.useMemo(() => (
    streams.reduce((acc:{label: string, value: string}[], stream: Stream) => {
      stream.categories?.forEach((category: string) => {
        if (!acc.find((option: { value: string }) => option.value === category)) acc.push({ label: category, value: category });
      });

      return acc;
    }, []).sort((a, b) => defaultCompare(a.value, b.value))), [streams]);

  const [cronDescription, setCronDescription] = useState<string>(currentConfig.cron_expression ? describeExpression(currentConfig.cron_expression) : '');

  const validateQueryString = useCallback(
    (queryString: ElasticsearchQueryString | string,
      streamIds: Array<string>,
      timeRange: RelativeTimeRangeWithEnd,
      timezone: string) => {
      const request = {
        timeRange: timeRange,
        queryString: queryString,
        streams: streamIds,
      };

      validateQuery(request, timezone).then((result) => {
        if (result?.status === 'WARNING' || result?.status === 'ERROR') {
          setFieldWarning('queryString', result);
        } else {
          setFieldWarning('queryString', undefined);
        }
      });
    }, [setFieldWarning]);

  const toTimeRange = (from) => ({
    type: 'relative',
    from: from / 1000,
  } as RelativeTimeRangeWithEnd);

  useEffect(() => {
    if (userCanViewLookupTables()) {
      LookupTablesActions.searchPaginated(1, 0, undefined, false);
    }
  }, [userCanViewLookupTables]);

  useEffect(() => {
    validateQueryString(
      eventDefinition.config.query,
      eventDefinition.config.streams,
      toTimeRange(eventDefinition.config.search_within_ms),
      userTimezone);
  }, [
    eventDefinition.config.query,
    eventDefinition.config.streams,
    eventDefinition.config.search_within_ms,
    setFieldWarning,
    userTimezone,
    validateQueryString]);

  const getUpdatedConfig = (key, value) => {
    const config = cloneDeep(eventDefinition.config);
    config[key] = value;
    setCurrentConfig(config);

    return config;
  };

  const propagateChange = (config: EventDefinitionConfig) => {
    onChange('config', config);
  };

  const formatStreamIds = memoize(
    (streamIds) => streamIds
      .map((streamId) => streams.find((s) => s.id === streamId) || streamId)
      .map((streamOrId) => {
        const stream = (typeof streamOrId === 'object' ? streamOrId : { title: streamOrId, id: streamOrId });

        return {
          label: stream.title,
          value: stream.id,
        };
      })
      .sort((s1, s2) => naturalSortIgnoreCase(s1.label, s2.label)),
    (streamIds) => streamIds.join('-'),
  );

  const syncParamsWithQuery = (paramsInQuery, config: EventDefinitionConfig) => {
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

    const newParameters = [];

    paramsInQuery.forEach((np) => {
      if (!keptParameters.find((p) => p.name === np)) {
        if (queryParameterStash[np]) {
          newParameters.push(queryParameterStash[np]);
        } else {
          newParameters.push(buildNewParameter(np));
        }
      }
    });

    setQueryParameterStash(merge(queryParameterStash, staleParameters));

    onChange('config', { ...config, query_parameters: keptParameters.concat(newParameters) });
  };

  const parseQuery = (queryString: string, config: EventDefinitionConfig, searchFilters = OrderedMap()) => {
    if (!userCanViewLookupTables()) {
      return;
    }

    const queryBuilder = Query.builder()
      .id(queryId)
      .query({ type: 'elasticsearch', query_string: queryString })
      .timerange({ type: 'relative', range: 1000 })
      .filters(searchFilters.toList() as FiltersType)
      .searchTypes([{
        id: searchTypeId,
        type: 'messages',
        limit: 10,
        offset: 0,
        filter: undefined,
        filters: undefined,
        name: undefined,
        query: undefined,
        timerange: undefined,
        streams: undefined,
        stream_categories: undefined,
        sort: [],
        decorators: [],
      }]);

    const query = queryBuilder.build();

    const search = Search.create().toBuilder()
      .queries([query])
      .build();

    parseSearch(search).then((res) => {
      syncParamsWithQuery(res.undeclared, config);
    });
  };

  const debouncedParseQuery = debounce(parseQuery, 250);

  const handleConfigChange = (name: string, config: EventDefinitionConfig) => {
    if (name === '_is_scheduled') {
      sendTelemetry(TELEMETRY_EVENT_TYPE.EVENTDEFINITION_CONDITION.FILTER_EXECUTED_AUTOMATICALLY_TOGGLED, {
        app_pathname: getPathnameWithoutId(pathname),
        app_section: 'event-definition-condition',
        app_action_value: 'enable-checkbox',
        is_scheduled: config._is_scheduled,
      });
    }

    propagateChange(config);
  };

  const handleQueryChange = (event) => {
    const { name } = event.target;
    const value = FormsUtils.getValueFromInput(event.target);
    const newConfig = getUpdatedConfig(name, value);
    handleConfigChange(name, newConfig);
    debouncedParseQuery(value, newConfig);
  };

  const handleCronExpressionChange = (event) => {
    const { name } = event.target;
    const value = FormsUtils.getValueFromInput(event.target);
    const newConfig = getUpdatedConfig(name, value);
    handleConfigChange(name, newConfig);
  };

  const handleCronTimezoneChange = (tz) => {
    const newConfig = getUpdatedConfig('cron_timezone', tz);
    handleConfigChange('cron_timezone', newConfig);
  };

  const handleSearchFiltersChange = (searchFilters) => {
    const { query } = eventDefinition.config;

    const newConfig = getUpdatedConfig('filters', searchFilters.toArray());
    propagateChange(newConfig);

    debouncedParseQuery(query, newConfig);
  };

  const handleEnabledChange = (event) => {
    const { name } = event.target;
    const value = FormsUtils.getValueFromInput(event.target);
    const newConfig = getUpdatedConfig(name, value);
    handleConfigChange(name, newConfig);
  };

  const handleUseCronSchedulingChange = (event) => {
    const { name } = event.target;
    const value = FormsUtils.getValueFromInput(event.target);
    const newConfig = cloneDeep(eventDefinition.config);
    newConfig[name] = value;

    if (value) {
      newConfig.cron_expression = '';
      newConfig.cron_timezone = userTimezone;
    } else {
      newConfig.cron_expression = null;
      newConfig.cron_timezone = null;
    }

    setCurrentConfig(newConfig);
    propagateChange(newConfig);
  };

  const hideFiltersPreview = (value) => {
    Store.set(PLUGGABLE_CONTROLS_HIDDEN_KEY, value);
    setSearchFiltersHidden(value);
  };

  const handleStreamsChange = (nextValue) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.EVENTDEFINITION_CONDITION.FILTER_STREAM_SELECTED, {
      app_pathname: getPathnameWithoutId(pathname),
      app_section: 'event-definition-condition',
      app_action_value: 'stream-select',
    });

    propagateChange(getUpdatedConfig('streams', nextValue));
  };

  const handleTimeRangeChange = (fieldName) => (nextValue, nextUnit) => {
    if (fieldName === 'search_within_ms' && nextUnit !== searchWithinMsUnit) {
      sendTelemetry(TELEMETRY_EVENT_TYPE.EVENTDEFINITION_CONDITION.FILTER_SEARCH_WITHIN_THE_LAST_UNIT_CHANGED, {
        app_pathname: getPathnameWithoutId(pathname),
        app_section: 'event-definition-condition',
        app_action_value: 'searchWithinMsUnit-select',
        new_unit: nextUnit,
      });
    } else if (fieldName === 'execute_every_ms' && nextUnit !== executeEveryMsUnit) {
      sendTelemetry(TELEMETRY_EVENT_TYPE.EVENTDEFINITION_CONDITION.FILTER_EXECUTE_SEARCH_EVERY_UNIT_CHANGED, {
        app_pathname: getPathnameWithoutId(pathname),
        app_section: 'event-definition-condition',
        app_action_value: 'executeEveryMsUnit-select',
        new_unit: nextUnit,
      });
    }

    const durationInMs = moment.duration(max([nextValue, 1]), nextUnit).asMilliseconds();

    propagateChange(getUpdatedConfig(fieldName, durationInMs));

    const stateFieldName = camelCase(fieldName);

    if (stateFieldName === 'searchWithinMs') {
      setSearchWithinMsDuration(nextValue);
      setSearchWithinMsUnit(nextUnit);

      return;
    }

    setExecuteEveryMsDuration(nextValue);
    setExecuteEveryMsUnit(nextUnit);
  };

  const renderQueryParameters = () => {
    const queryParameters = eventDefinition?.config?.query_parameters || [];

    const onChangeQueryParameters = (newQueryParameters) => {
      const newConfig = { ...eventDefinition.config, query_parameters: newQueryParameters || [] };

      return onChange('config', newConfig);
    };

    if (!userCanViewLookupTables()) {
      return (
        <Alert bsStyle="info">
          This account lacks permission to declare Query Parameters from Lookup Tables.
        </Alert>
      );
    }

    const parameterButtons = queryParameters.map((queryParam) => (
      <EditQueryParameterModal key={queryParam.name}
                               queryParameter={LookupTableParameter.fromJSON(queryParam)}
                               embryonic={!!(queryParam as LookupTableParameterJsonEmbryonic).embryonic}
                               queryParameters={queryParameters}
                               lookupTables={Object.values(tables || {})}
                               onChange={onChangeQueryParameters} />
    ));

    if (isEmpty(parameterButtons)) {
      return null;
    }

    const hasEmbryonicParameters = !isEmpty(queryParameters.filter((param : LookupTableParameterJsonEmbryonic) => (param.embryonic)));

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

  const streamCategorySelector = () => {
    if (!streamCategoryOptions || streamCategoryOptions.length === 0) return null;

    return (
      <FormGroup controlId="filter-stream-categories">
        <ControlLabel>Stream Categories <small className="text-muted">(Optional)</small></ControlLabel>
        <MultiSelect id="filter-stream-categories"
                     matchProp="label"
                     onChange={(selected) => propagateChange(getUpdatedConfig('stream_categories', selected === '' ? [] : selected.split(',')))}
                     options={streamCategoryOptions}
                     value={defaultTo(eventDefinition.config.stream_categories, []).join(',')} />
        <HelpBlock>Select stream categories the search should include.</HelpBlock>
      </FormGroup>
    );
  };

  const warmTierTimeStamp = () => {
    const latestWarmTierRangeEnd = warmTierRanges.map((range) => range.end).sort((a, b) => b - a)[0];

    return <RelativeTime dateTime={latestWarmTierRangeEnd} />;
  };

  const onlyFilters = eventDefinition._scope === 'ILLUMINATE';

  // Ensure deleted streams are still displayed in select
  const allStreamIds = union(streams.map((s) => s.id), defaultTo(eventDefinition.config.streams, []));
  const formattedStreams = formatStreamIds(allStreamIds);

  return (
    <fieldset>
      <h2 className={commonStyles.title}>Filter</h2>
      <p>Add information to filter the log messages that are relevant for this Event Definition.</p>
      {onlyFilters || (
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
             value={defaultTo(currentConfig.query, '')}
             onChange={handleQueryChange} />
      )}

      {onlyFilters || renderQueryParameters()}

      {!searchFiltersHidden && (
      <FormGroup controlId="search-filters">
        <ControlLabel>Search Filters <small className="text-muted">(Optional)</small></ControlLabel>
        <div style={{ margin: '16px 0' }}>
          <SearchFiltersFormControls filters={eventDefinition.config.filters}
                                     onChange={handleSearchFiltersChange}
                                     hideFiltersPreview={hideFiltersPreview} />
        </div>
      </FormGroup>
      )}

      {onlyFilters || (
      <>
        <FormGroup controlId="filter-streams">
          <ControlLabel>Streams <small className="text-muted">(Optional)</small></ControlLabel>
          <MultiSelect id="filter-streams"
                       matchProp="label"
                       onChange={(selected) => handleStreamsChange(selected === '' ? [] : selected.split(','))}
                       options={formattedStreams}
                       value={defaultTo(eventDefinition.config.streams, []).join(',')} />
          <HelpBlock>Select streams the search should include. Searches in all streams if empty.</HelpBlock>
        </FormGroup>
        {streamCategorySelector()}
        {isSearchingWarmTier(warmTierRanges) && (
        <Alert bsStyle="danger" title="Warm Tier Warning">
          The selected time range will include data stored in the Warm Tier. Events that must frequently retrieve data from the Warm Tier may cause performance problems.
          A value for <strong>Search within the last</strong> exceeding the following duration will fall into the Warm Tier: {warmTierTimeStamp()}.
        </Alert>
        )}
        <FormGroup controlId="search-within" validationState={validation.errors.search_within_ms ? 'error' : null}>
          <TimeUnitInput label="Search within the last"
                         update={handleTimeRangeChange('search_within_ms')}
                         value={searchWithinMsDuration}
                         unit={searchWithinMsUnit}
                         units={TIME_UNITS}
                         clearable
                         required />
          {validation.errors.search_within_ms && (
          <HelpBlock>{get(validation, 'errors.search_within_ms[0]')}</HelpBlock>
          )}
        </FormGroup>
        <Input id="is-cron-checkbox"
               type="checkbox"
               name="use_cron_scheduling"
               label="Use Cron Scheduling"
               help="Schedule this event with a Quartz cron expression"
               checked={defaultTo(eventDefinition.config.use_cron_scheduling, false)}
               onChange={handleUseCronSchedulingChange} />
        {currentConfig.use_cron_scheduling
          ? (
            <>
              <FormGroup controlId="cron-expression" validationState={validation.errors.cron_expression ? 'error' : null}>
                <Input id="cron-expression"
                       name="cron_expression"
                       label="Cron Expression"
                       type="text"
                       help={(
                         <span>
                           {cronDescription || 'A Quartz cron expression to determine when the event should be run.'}
                         </span>
                   )}
                       value={defaultTo(currentConfig.cron_expression, '')}
                       onBlur={() => setCronDescription(describeExpression(currentConfig.cron_expression))}
                       onChange={handleCronExpressionChange} />
                {validation.errors.cron_expression && (
                <HelpBlock>{get(validation, 'errors.cron_expression[0]')}</HelpBlock>
                )}
              </FormGroup>
              <FormGroup>
                <ControlLabel>Cron Time Zone</ControlLabel>
                <TimezoneSelect value={defaultTo(currentConfig.cron_timezone, userTimezone)}
                                name="cron_timezone"
                                clearable={false}
                                onChange={handleCronTimezoneChange} />

              </FormGroup>
            </>
          )
          : (
            <FormGroup controlId="execute-every" validationState={validation.errors.execute_every_ms ? 'error' : null}>
              <TimeUnitInput label="Execute search every"
                             update={handleTimeRangeChange('execute_every_ms')}
                             value={executeEveryMsDuration}
                             unit={executeEveryMsUnit}
                             units={TIME_UNITS}
                             clearable
                             required />
              {validation.errors.execute_every_ms && (
              <HelpBlock>{get(validation, 'errors.execute_every_ms[0]')}</HelpBlock>
              )}
            </FormGroup>
          )}

        <Input id="schedule-checkbox"
               type="checkbox"
               name="_is_scheduled"
               label="Enable"
               help="Should this event definition be executed automatically?"
               checked={defaultTo(eventDefinition.config._is_scheduled, true)}
               onChange={handleEnabledChange} />
      </>
      )}
    </fieldset>
  );
};

export default FilterForm;
