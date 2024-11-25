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
import { useEffect, useMemo, useState } from 'react';
import moment from 'moment';
import Immutable from 'immutable';

import { useStore } from 'stores/connect';
import type { Store } from 'stores/StoreTypes';
import { ConfigurationsActions, ConfigurationsStore } from 'stores/configurations/ConfigurationsStore';
import { getConfig } from 'components/configurations/helpers';
import { ConfigurationType } from 'components/configurations/ConfigurationTypes';
import { Button, Row, Col, BootstrapModalForm, Input } from 'components/bootstrap';
import { IfPermitted, ISODurationInput } from 'components/common';
import Spinner from 'components/common/Spinner';
import type { SearchConfig } from 'components/search';
import Select from 'components/common/Select/Select';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import 'moment-duration-format';
import type { TimeRangePreset } from 'components/configurations/TimeRangePresetForm';
import TimeRangePresetForm from 'components/configurations/TimeRangePresetForm';
import generateId from 'logic/generateId';
import TimeRangePresetOptionsSummary from 'components/configurations/TimeRangePresetOptionSummary';
import { onInitializingTimerange } from 'views/components/TimerangeForForm';
import useUserDateTime from 'hooks/useUserDateTime';
import type { DateTime, DateTimeFormats } from 'util/DateTime';
import { normalizeFromSearchBarForBackend } from 'views/logic/queries/NormalizeTimeRange';
import { getPathnameWithoutId } from 'util/URLUtils';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import useMinimumRefreshInterval from 'views/hooks/useMinimumRefreshInterval';
import Alert from 'components/bootstrap/Alert';
import useLocation from 'routing/useLocation';
import ReadableDuration from 'components/common/ReadableDuration';

import TimeRangeOptionsForm from './TimeRangeOptionsForm';
import TimeRangeOptionsSummary from './TimeRangeOptionsSummary';

const queryTimeRangeLimitValidator = (milliseconds: number) => milliseconds >= 1;

const surroundingTimeRangeValidator = (milliseconds: number) => milliseconds >= 1;

const autoRefreshTimeRangeValidator = (minimumRefreshIntervalMS: number) => (milliseconds: number) => milliseconds >= 1000 && milliseconds >= minimumRefreshIntervalMS;

const splitStringList = (stringList: string) => stringList.split(',').map((f) => f.trim()).filter((f) => f.length > 0);

const buildTimeRangeOptions = (options: { [x: string]: string; }) => Object.keys(options).map((key) => ({ period: key, description: options[key] }));

type Option = { period: string, description: string };

const mapQuickAccessBEData = (items: Array<TimeRangePreset>, formatTime: (time: DateTime, format?: DateTimeFormats) => string): Immutable.List<TimeRangePreset> => Immutable.List(items.map(({ timerange, description, id }) => {
  const presetId = id ?? generateId();

  return { description, id: presetId, timerange: onInitializingTimerange(timerange, formatTime) };
}));

const SearchesConfig = () => {
  const { userTimezone, formatTime } = useUserDateTime();
  const isLimitEnabled = (config: { query_time_range_limit: number }) => moment.duration(config?.query_time_range_limit).asMilliseconds() > 0;
  const { data: minimumRefreshInterval, isInitialLoading: isLoadingMinimumRefreshInterval } = useMinimumRefreshInterval();
  const minimumRefreshIntervalMS = moment.duration(minimumRefreshInterval).asMilliseconds();
  const [showConfigModal, setShowConfigModal] = useState<boolean>(false);
  const [viewConfig, setViewConfig] = useState<SearchConfig | undefined>(undefined);
  const [formConfig, setFormConfig] = useState<SearchConfig | undefined>(undefined);
  const configuration = useStore(ConfigurationsStore as Store<Record<string, any>>, (state) => state?.configuration);
  const [relativeTimeRangeOptionsUpdate, setRelativeTimeRangeOptionsUpdate] = useState<Array<Option> | undefined>(undefined);
  const [surroundingTimeRangeOptionsUpdate, setSurroundingTimeRangeOptionsUpdate] = useState<Array<Option> | undefined>(undefined);
  const [autoRefreshTimeRangeOptionsUpdate, setAutoRefreshTimeRangeOptionsUpdate] = useState<Array<Option> | undefined>(undefined);
  const [surroundingFilterFieldsUpdate, setSurroundingFilterFieldsUpdate] = useState<string | undefined>(undefined);
  const [analysisDisabledFieldsUpdate, setAnalysisDisabledFieldsUpdate] = useState<string | undefined>(undefined);
  const [defaultAutoRefreshOptionUpdate, setDefaultAutoRefreshOptionUpdate] = useState<string | undefined>(undefined);
  const [timeRangePresetsUpdated, setTimeRangePresetsUpdated] = useState<Immutable.List<TimeRangePreset>>(undefined);
  const [showCancelAfterSeconds, setShowCancelAfterSeconds] = useState(false);
  const sendTelemetry = useSendTelemetry();
  const { pathname } = useLocation();

  useEffect(() => {
    ConfigurationsActions.list(ConfigurationType.SEARCHES_CLUSTER_CONFIG).then(() => {
      const config = getConfig(ConfigurationType.SEARCHES_CLUSTER_CONFIG, configuration);
      setViewConfig(config);
      setFormConfig(config);
      setShowCancelAfterSeconds(!!config?.cancel_after_seconds);
    });
  }, [configuration]);

  const onUpdate = (field: keyof SearchConfig) => (newOptions) => {
    setFormConfig({ ...formConfig, [field]: newOptions });
  };

  const onTimeRangePresetsUpdate = (data: Immutable.List<TimeRangePreset>) => {
    setTimeRangePresetsUpdated(data);
  };

  const onSurroundingTimeRangeOptionsUpdate = (data: Array<Option>) => {
    setSurroundingTimeRangeOptionsUpdate(data);
  };

  const onAutoRefreshTimeRangeOptionsUpdate = (data: Array<Option>) => {
    setAutoRefreshTimeRangeOptionsUpdate(data);
  };

  const onFilterFieldsUpdate = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSurroundingFilterFieldsUpdate(e.target.value);
  };

  const onAnalysisDisabledFieldsUpdate = (e: React.ChangeEvent<HTMLInputElement>) => {
    setAnalysisDisabledFieldsUpdate(e.target.value);
  };

  const onAutoRefreshDefaultOptionsUpdate = (data: string) => {
    setDefaultAutoRefreshOptionUpdate(data);
  };

  const onChecked = () => {
    let queryTimeRangeLimit;

    if (isLimitEnabled(formConfig)) {
      // If currently enabled, disable by setting the limit to 0 seconds.
      queryTimeRangeLimit = 'PT0S';
    } else {
      // If currently not enabled, set a default of 30 days.
      queryTimeRangeLimit = 'P30D';
    }

    setFormConfig({ ...formConfig, query_time_range_limit: queryTimeRangeLimit });
  };

  const onCancelAfterSecondsChanged = ({ target: { value } }: React.ChangeEvent<HTMLInputElement>) => {
    setFormConfig({ ...formConfig, cancel_after_seconds: value });
  };

  const onCheckedCancelAfterSeconds = () => {
    let cancelAfterSeconds: number | null;

    if (showCancelAfterSeconds) {
      cancelAfterSeconds = null;
    } else {
      cancelAfterSeconds = 30;
    }

    setShowCancelAfterSeconds((cur) => !cur);
    setFormConfig({ ...formConfig, cancel_after_seconds: cancelAfterSeconds });
  };

  const openModal = () => {
    setShowConfigModal(true);
  };

  const resetFormUpdates = () => {
    setRelativeTimeRangeOptionsUpdate(undefined);
    setSurroundingTimeRangeOptionsUpdate(undefined);
    setSurroundingFilterFieldsUpdate(undefined);
    setAnalysisDisabledFieldsUpdate(undefined);
    setAutoRefreshTimeRangeOptionsUpdate(undefined);
    setDefaultAutoRefreshOptionUpdate(undefined);
    setTimeRangePresetsUpdated(undefined);
  };

  const handleModalCancel = () => {
    setShowConfigModal(false);
    setFormConfig(viewConfig);
    resetFormUpdates();
  };

  const saveConfig = () => {
    const update = { ...formConfig };

    sendTelemetry(TELEMETRY_EVENT_TYPE.CONFIGURATIONS.SEARCHES_UPDATED, {
      app_pathname: getPathnameWithoutId(pathname),
      app_section: 'search',
      app_action_value: 'configuration-save',
    });

    if (relativeTimeRangeOptionsUpdate) {
      update.relative_timerange_options = {};

      relativeTimeRangeOptionsUpdate.forEach((entry) => {
        update.relative_timerange_options[entry.period] = entry.description;
      });

      setRelativeTimeRangeOptionsUpdate(undefined);
    }

    if (timeRangePresetsUpdated) {
      update.quick_access_timerange_presets = timeRangePresetsUpdated.toArray().map(({ description, timerange, id }) => ({
        description, timerange: normalizeFromSearchBarForBackend(timerange, userTimezone), id,
      }));

      setTimeRangePresetsUpdated(undefined);
    }

    if (surroundingTimeRangeOptionsUpdate) {
      update.surrounding_timerange_options = {};

      surroundingTimeRangeOptionsUpdate.forEach((entry) => {
        update.surrounding_timerange_options[entry.period] = entry.description;
      });

      setSurroundingTimeRangeOptionsUpdate(undefined);
    }

    if (surroundingFilterFieldsUpdate) {
      update.surrounding_filter_fields = splitStringList(surroundingFilterFieldsUpdate);
      setSurroundingFilterFieldsUpdate(undefined);
    }

    if (analysisDisabledFieldsUpdate) {
      update.analysis_disabled_fields = splitStringList(analysisDisabledFieldsUpdate);
      setAnalysisDisabledFieldsUpdate(undefined);
    }

    if (autoRefreshTimeRangeOptionsUpdate) {
      update.auto_refresh_timerange_options = Object.fromEntries(autoRefreshTimeRangeOptionsUpdate.map((entry) => [entry.period, entry.description]));
      setAutoRefreshTimeRangeOptionsUpdate(undefined);
    }

    const defaultAutoRefreshOption = defaultAutoRefreshOptionUpdate
      ? update.auto_refresh_timerange_options[defaultAutoRefreshOptionUpdate] ?? Object.keys(update.auto_refresh_timerange_options)[0]
      : update.auto_refresh_timerange_options[update.default_auto_refresh_option] ?? Object.keys(update.auto_refresh_timerange_options)[0];

    if (update.default_auto_refresh_option !== defaultAutoRefreshOption) {
      update.default_auto_refresh_option = defaultAutoRefreshOptionUpdate;
      setDefaultAutoRefreshOptionUpdate(undefined);
    }

    const newFormConfig = { ...formConfig, ...update };

    ConfigurationsActions.update(ConfigurationType.SEARCHES_CLUSTER_CONFIG, newFormConfig).then(() => {
      setShowConfigModal(false);
      resetFormUpdates();
    });
  };

  const timeRangePresetsFromBE = useMemo(() => mapQuickAccessBEData(formConfig?.quick_access_timerange_presets ?? [], formatTime), [formConfig?.quick_access_timerange_presets, formatTime]);
  const timeRangePresets = useMemo(() => timeRangePresetsUpdated ?? timeRangePresetsFromBE, [timeRangePresetsFromBE, timeRangePresetsUpdated]);

  if (!viewConfig) {
    return <Spinner />;
  }

  const duration = (config) => moment.duration(config.query_time_range_limit);
  const limit = (config) => (isLimitEnabled(config) ? `${config.query_time_range_limit} (${duration(config).humanize()})` : 'disabled');
  const autoRefreshOptions = (config) => autoRefreshTimeRangeOptionsUpdate ?? buildTimeRangeOptions(config.auto_refresh_timerange_options);
  const formDefaultAutoRefreshOptionUpdate = (config) => defaultAutoRefreshOptionUpdate ?? config.default_auto_refresh_option;
  const defaultAutoRefreshOption = (config) => (autoRefreshOptions(config).find((option) => option.period === formDefaultAutoRefreshOptionUpdate(config))
    ? formDefaultAutoRefreshOptionUpdate(config)
    : autoRefreshOptions[0]?.period);

  const cancellationTimeout = (config) => (config.cancel_after_seconds ? `${config.cancel_after_seconds} seconds` : 'disabled');

  return (
    <div>
      <h2>Search Configuration</h2>

      <dl className="deflist">
        <dt>Query time range limit</dt>
        <dd>{limit(viewConfig)}</dd>
        <dd>The maximum time users can query data in the past. This prevents users from accidentally creating queries
          which
          span a lot of data and would need a long time and many resources to complete (if at all).
        </dd>
      </dl>

      <dl className="deflist">
        <dt>Cancellation timeout</dt>
        <dd>{cancellationTimeout(viewConfig)}</dd>
        <dd>The time in seconds per widget after which search execution will be canceled automatically.
          This minimizes the amount of executed searches and improves performance.
        </dd>
      </dl>

      <Row>
        <Col md={4}>
          <strong>Search Time Range Presets</strong>
          <TimeRangePresetOptionsSummary options={timeRangePresetsFromBE.toArray()} />
          <strong>Surrounding time range options</strong>
          <TimeRangeOptionsSummary options={viewConfig.surrounding_timerange_options} />
        </Col>
        <Col md={4}>
          <Row style={{ marginBottom: 20 }}>
            <Col>
              <strong>Surrounding search filter fields</strong>
              <ul>
                {viewConfig.surrounding_filter_fields && viewConfig.surrounding_filter_fields.map((f: string) => (
                  <li key={f}>{f}
                  </li>
                ))}
              </ul>
            </Col>
          </Row>
          <Row>
            <Col>
              <strong>UI analysis disabled for fields</strong>
              <ul>
                {viewConfig.analysis_disabled_fields && (viewConfig.analysis_disabled_fields.map((f: string) => (
                  <li key={f}>{f}
                  </li>
                )))}
              </ul>
            </Col>
          </Row>
        </Col>
        <Col md={4}>
          <strong>Auto-refresh interval options</strong>
          <TimeRangeOptionsSummary options={viewConfig.auto_refresh_timerange_options} />

          <strong>Default auto-refresh interval</strong>
          <TimeRangeOptionsSummary options={{ [viewConfig.default_auto_refresh_option]: viewConfig.auto_refresh_timerange_options[viewConfig.default_auto_refresh_option] }} />
        </Col>
      </Row>
      <IfPermitted permissions="clusterconfigentry:edit">
        <Button bsStyle="info" bsSize="xs" onClick={openModal}>Edit configuration</Button>
      </IfPermitted>

      {showConfigModal && formConfig && (
        <BootstrapModalForm show
                            bsSize="large"
                            title="Update Search Configuration"
                            onSubmitForm={saveConfig}
                            onCancel={handleModalCancel}
                            submitButtonText="Update configuration">
          <fieldset>
            <label htmlFor="query-limit-checkbox">Query Time Range Limit</label>
            <Input id="query-limit-checkbox"
                   type="checkbox"
                   label="Enable query limit"
                   name="enabled"
                   checked={isLimitEnabled(formConfig)}
                   onChange={onChecked} />
            {isLimitEnabled(formConfig) && (
              <ISODurationInput id="query-timerange-limit-field"
                                duration={formConfig.query_time_range_limit}
                                update={onUpdate('query_time_range_limit')}
                                label="Query time range limit (ISO8601 Duration)"
                                help='The maximum time range for searches. (i.e. "P30D" for 30 days, "PT24H" for 24 hours)'
                                validator={queryTimeRangeLimitValidator}
                                required />
            )}
            <label htmlFor="cancel_after_seconds_checkbox">Query Cancellation Timeout</label>
            <Input id="cancel_after_seconds_checkbox"
                   type="checkbox"
                   label="Enable query cancellation timeout"
                   name="cancel_after_seconds_checkbox"
                   checked={showCancelAfterSeconds}
                   onChange={onCheckedCancelAfterSeconds}
                   help="The time in seconds per widget after which search execution will be canceled automatically. This minimizes the amount of executed searches and improves performance." />
            {showCancelAfterSeconds && (
              <Input id="cancel_after_seconds"
                     type="number"
                     label="Cancellation timeout"
                     name="cancel_after_seconds"
                     min="1"
                     step="1"
                     pattern="\d+"
                     required
                     value={formConfig.cancel_after_seconds}
                     onChange={onCancelAfterSecondsChanged} />
            )}
            <TimeRangePresetForm options={timeRangePresets} onUpdate={onTimeRangePresetsUpdate} />
            <TimeRangeOptionsForm options={surroundingTimeRangeOptionsUpdate || buildTimeRangeOptions(formConfig.surrounding_timerange_options)}
                                  update={onSurroundingTimeRangeOptionsUpdate}
                                  validator={surroundingTimeRangeValidator}
                                  title="Surrounding Timerange Options"
                                  help={
                                    <span>Configure the available options for the <strong>surrounding</strong> time range selector as <strong>ISO8601 duration</strong></span>
              } />

            <Input id="filter-fields-input"
                   type="text"
                   label="Surrounding search filter fields"
                   onChange={onFilterFieldsUpdate}
                   value={surroundingFilterFieldsUpdate || (formConfig.surrounding_filter_fields && formConfig.surrounding_filter_fields.join(', '))}
                   help="A ',' separated list of message fields that will be used as filter for the surrounding messages query."
                   required />

            <Input id="disabled-fields-input"
                   type="text"
                   label="Disabled analysis fields"
                   onChange={onAnalysisDisabledFieldsUpdate}
                   value={analysisDisabledFieldsUpdate || (formConfig.analysis_disabled_fields && formConfig.analysis_disabled_fields.join(', '))}
                   help="A ',' separated list of message fields for which analysis features like QuickValues will be disabled in the web UI."
                   required />
            <TimeRangeOptionsForm options={autoRefreshOptions(formConfig)}
                                  update={onAutoRefreshTimeRangeOptionsUpdate}
                                  validator={autoRefreshTimeRangeValidator(minimumRefreshIntervalMS)}
                                  title="Auto-Refresh Interval Options"
                                  help={<span>Configure the available options for the <strong>auto-refresh</strong> interval selector as <strong>ISO8601 duration</strong></span>} />
            <Input label="Default Auto-Refresh Option"
                   id="default-auto-refresh-option"
                   required
                   help="Select the interval which is used when auto-refresh is started without explicitly selecting one">
              <Select placeholder="Select the default interval"
                      clearable={false}
                      options={autoRefreshOptions(formConfig)}
                      displayKey="description"
                      valueKey="period"
                      matchProp="label"
                      onChange={onAutoRefreshDefaultOptionsUpdate}
                      value={defaultAutoRefreshOption(formConfig)} />
            </Input>
            {!isLoadingMinimumRefreshInterval && (
              <Alert bsStyle="warning">
                Please note, a minimum refresh interval of <ReadableDuration duration={minimumRefreshInterval} /> ({minimumRefreshInterval}) has been configured in the graylog.conf.
                Only intervals which are equal or above the minimum can be used.
              </Alert>
            )}
          </fieldset>
        </BootstrapModalForm>
      )}
    </div>
  );
};

export default SearchesConfig;
