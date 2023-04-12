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
import PropTypes from 'prop-types';
import React from 'react';
import moment from 'moment';

import { Button, Row, Col, BootstrapModalForm, Input } from 'components/bootstrap';
import { IfPermitted, ISODurationInput } from 'components/common';
import ObjectUtils from 'util/ObjectUtils';
import type { SearchesConfig as SearchesConfigType } from 'components/search/SearchConfig';
import Select from 'components/common/Select/Select';
import withTelemetry from 'logic/telemetry/withTelemetry';
import type { TelemetryEvent, TelemetryEventType } from 'logic/telemetry/TelemetryContext';

import 'moment-duration-format';

import TimeRangeOptionsForm from './TimeRangeOptionsForm';
import TimeRangeOptionsSummary from './TimeRangeOptionsSummary';

function _queryTimeRangeLimitValidator(milliseconds: number) {
  return milliseconds >= 1;
}

function _relativeTimeRangeValidator(milliseconds: number, duration: string) {
  return milliseconds >= 1 || duration === 'PT0S';
}

function _surroundingTimeRangeValidator(milliseconds: number) {
  return milliseconds >= 1;
}

function _autoRefreshTimeRangeValidator(milliseconds: number) {
  return milliseconds >= 1000;
}

function _splitStringList(stringList: string) {
  return stringList.split(',').map((f) => f.trim()).filter((f) => f.length > 0);
}

const _buildTimeRangeOptions = (options: { [x: string]: string; }) => {
  return Object.keys(options).map((key) => {
    return { period: key, description: options[key] };
  });
};

type Option = { period: string, description: string };

type Props = {
  config: SearchesConfigType,
  updateConfig: (newConfig: SearchesConfigType) => Promise<unknown>,
  sendTelemetry: (eventType: TelemetryEventType, event: TelemetryEvent) => void,
};
type State = {
  config: SearchesConfigType,
  showConfigModal: boolean,
  limitEnabled: boolean,
  relativeTimeRangeOptionsUpdate: Array<Option>,
  surroundingTimeRangeOptionsUpdate: Array<Option>,
  autoRefreshTimeRangeOptionsUpdate: Array<Option>,
  surroundingFilterFields?: string,
  analysisDisabledFields?: string,
  defaultAutoRefreshOptionUpdate?: string,
};

class SearchesConfig extends React.Component<Props, State> {
  private readonly defaultState: State;

  static propTypes = {
    config: PropTypes.exact({
      query_time_range_limit: PropTypes.string,
      relative_timerange_options: PropTypes.objectOf(PropTypes.string),
      surrounding_timerange_options: PropTypes.objectOf(PropTypes.string),
      surrounding_filter_fields: PropTypes.arrayOf(PropTypes.string),
      analysis_disabled_fields: PropTypes.arrayOf(PropTypes.string),
      auto_refresh_timerange_options: PropTypes.objectOf(PropTypes.string),
      default_auto_refresh_option: PropTypes.string,
    }).isRequired,
    updateConfig: PropTypes.func.isRequired,
    sendTelemetry: PropTypes.func.isRequired,
  };

  constructor(props: Props) {
    super(props);

    const { config } = props;

    const queryTimeRangeLimit = config?.query_time_range_limit;

    this.state = {
      showConfigModal: false,
      config: { ...config },
      limitEnabled: moment.duration(queryTimeRangeLimit).asMilliseconds() > 0,
      relativeTimeRangeOptionsUpdate: undefined,
      surroundingTimeRangeOptionsUpdate: undefined,
      autoRefreshTimeRangeOptionsUpdate: undefined,
      defaultAutoRefreshOptionUpdate: undefined,
    };

    this.defaultState = { ...this.state };
  }

  _onUpdate = (field: keyof SearchesConfigType) => {
    return (newOptions) => {
      const { config } = this.state;
      const update = ObjectUtils.clone(config);

      update[field] = newOptions;

      this.setState({ config: update });
    };
  };

  _onRelativeTimeRangeOptionsUpdate = (data: Array<Option>) => {
    this.setState({ relativeTimeRangeOptionsUpdate: data });
  };

  _onSurroundingTimeRangeOptionsUpdate = (data: Array<Option>) => {
    this.setState({ surroundingTimeRangeOptionsUpdate: data });
  };

  _onAutoRefreshTimeRangeOptionsUpdate = (data: Array<Option>) => {
    this.setState({ autoRefreshTimeRangeOptionsUpdate: data });
  };

  _onFilterFieldsUpdate = (e: React.ChangeEvent<HTMLInputElement>) => {
    this.setState({ surroundingFilterFields: e.target.value });
  };

  _onAnalysisDisabledFieldsUpdate = (e: React.ChangeEvent<HTMLInputElement>) => {
    this.setState({ analysisDisabledFields: e.target.value });
  };

  _onAutoRefreshDefaultOptionsUpdate = (data: string) => {
    this.setState({ defaultAutoRefreshOptionUpdate: data });
  };

  _onChecked = () => {
    const { config: origConfig, limitEnabled } = this.state;
    const config = ObjectUtils.clone(origConfig);

    if (limitEnabled) {
      // If currently enabled, disable by setting the limit to 0 seconds.
      config.query_time_range_limit = 'PT0S';
    } else {
      // If currently not enabled, set a default of 30 days.
      config.query_time_range_limit = 'P30D';
    }

    this.setState({ config: config, limitEnabled: !limitEnabled });
  };

  _saveConfig = () => {
    const { updateConfig, sendTelemetry } = this.props;
    const {
      analysisDisabledFields,
      config,
      relativeTimeRangeOptionsUpdate,
      surroundingTimeRangeOptionsUpdate,
      surroundingFilterFields,
      autoRefreshTimeRangeOptionsUpdate,
      defaultAutoRefreshOptionUpdate,
    } = this.state;
    const update = ObjectUtils.clone(config);

    sendTelemetry('submit_form', {
      appSection: 'configurations_search',
      eventElement: 'update_configuration_button',
    });

    if (relativeTimeRangeOptionsUpdate) {
      update.relative_timerange_options = {};

      relativeTimeRangeOptionsUpdate.forEach((entry) => {
        update.relative_timerange_options[entry.period] = entry.description;
      });

      this.setState({ relativeTimeRangeOptionsUpdate: undefined });
    }

    if (surroundingTimeRangeOptionsUpdate) {
      update.surrounding_timerange_options = {};

      surroundingTimeRangeOptionsUpdate.forEach((entry) => {
        update.surrounding_timerange_options[entry.period] = entry.description;
      });

      this.setState({ surroundingTimeRangeOptionsUpdate: undefined });
    }

    // Make sure to update filter fields
    if (surroundingFilterFields) {
      update.surrounding_filter_fields = _splitStringList(surroundingFilterFields);
      this.setState({ surroundingFilterFields: undefined });
    }

    if (analysisDisabledFields) {
      update.analysis_disabled_fields = _splitStringList(analysisDisabledFields);
      this.setState({ analysisDisabledFields: undefined });
    }

    if (autoRefreshTimeRangeOptionsUpdate) {
      update.auto_refresh_timerange_options = Object.fromEntries(autoRefreshTimeRangeOptionsUpdate.map((entry) => [entry.period, entry.description]));
      this.setState({ autoRefreshTimeRangeOptionsUpdate: undefined });
    }

    const defaultAutoRefreshOption = defaultAutoRefreshOptionUpdate
      ? update.auto_refresh_timerange_options[defaultAutoRefreshOptionUpdate] ?? Object.keys(update.auto_refresh_timerange_options)[0]
      : update.auto_refresh_timerange_options[update.default_auto_refresh_option] ?? Object.keys(update.auto_refresh_timerange_options)[0];

    if (update.default_auto_refresh_option !== defaultAutoRefreshOption) {
      update.default_auto_refresh_option = defaultAutoRefreshOptionUpdate;
      this.setState({ defaultAutoRefreshOptionUpdate: undefined });
    }

    updateConfig(update).then(() => {
      this._closeModal();
    });
  };

  _openModal = () => {
    this.setState({ showConfigModal: true });
  };

  _closeModal = () => {
    // Reset to initial state when the modal is closed without saving.
    this.setState(this.defaultState);
  };

  render() {
    const {
      showConfigModal,
      config,
      limitEnabled,
      surroundingTimeRangeOptionsUpdate,
      surroundingFilterFields,
      relativeTimeRangeOptionsUpdate,
      analysisDisabledFields,
      autoRefreshTimeRangeOptionsUpdate,
    } = this.state;
    const duration = moment.duration(config.query_time_range_limit);
    const limit = limitEnabled ? `${config.query_time_range_limit} (${duration.humanize()})` : 'disabled';

    let filterFields;
    let filterFieldsString;

    if (config.surrounding_filter_fields) {
      filterFields = config.surrounding_filter_fields.map((f) => <li key={f}>{f}</li>);
      filterFieldsString = config.surrounding_filter_fields.join(', ');
    }

    let analysisDisabledFieldsListItems;
    let analysisDisabledFieldsString;

    if (config.analysis_disabled_fields) {
      analysisDisabledFieldsListItems = config.analysis_disabled_fields.map((f) => <li key={f}>{f}</li>);
      analysisDisabledFieldsString = config.analysis_disabled_fields.join(', ');
    }

    const autoRefreshOptions = autoRefreshTimeRangeOptionsUpdate ?? _buildTimeRangeOptions(config.auto_refresh_timerange_options);
    const defaultAutoRefreshOptionUpdate = this.state.defaultAutoRefreshOptionUpdate ?? config.default_auto_refresh_option;
    const defaultAutoRefreshOption = autoRefreshOptions.find((option) => option.period === defaultAutoRefreshOptionUpdate)
      ? defaultAutoRefreshOptionUpdate
      : autoRefreshOptions[0]?.period;

    return (
      <div>
        <h2>Search Configuration</h2>

        <dl className="deflist">
          <dt>Query time range limit</dt>
          <dd>{limit}</dd>
          <dd>The maximum time users can query data in the past. This prevents users from accidentally creating queries which
            span a lot of data and would need a long time and many resources to complete (if at all).
          </dd>
        </dl>

        <Row>
          <Col md={6}>
            <strong>Relative time range options</strong>
            <TimeRangeOptionsSummary options={config.relative_timerange_options} />
            <strong>Surrounding time range options</strong>
            <TimeRangeOptionsSummary options={config.surrounding_timerange_options} />
          </Col>
          <Col md={6} />
          <Col md={6}>
            <strong>Auto-refresh interval options</strong>
            <TimeRangeOptionsSummary options={config.auto_refresh_timerange_options} />

            <strong>Default auto-refresh interval</strong>
            <TimeRangeOptionsSummary options={{ [config.default_auto_refresh_option]: config.auto_refresh_timerange_options[config.default_auto_refresh_option] }} />

            <strong>Surrounding search filter fields</strong>
            <ul>
              {filterFields}
            </ul>

            <strong>UI analysis disabled for fields</strong>
            <ul>
              {analysisDisabledFieldsListItems}
            </ul>

          </Col>

        </Row>
        <IfPermitted permissions="clusterconfigentry:edit">
          <Button bsStyle="info" bsSize="xs" onClick={this._openModal}>Edit configuration</Button>
        </IfPermitted>

        <BootstrapModalForm show={showConfigModal}
                            title="Update Search Configuration"
                            onSubmitForm={this._saveConfig}
                            onCancel={this._closeModal}
                            submitButtonText="Update configuration">
          <fieldset>
            <label htmlFor="query-limit-checkbox">Relative Timerange Options</label>
            <Input id="query-limit-checkbox"
                   type="checkbox"
                   label="Enable query limit"
                   name="enabled"
                   checked={limitEnabled}
                   onChange={this._onChecked} />
            {limitEnabled && (
              <ISODurationInput id="query-timerange-limit-field"
                                duration={config.query_time_range_limit}
                                update={this._onUpdate('query_time_range_limit')}
                                label="Query time range limit (ISO8601 Duration)"
                                help={'The maximum time range for searches. (i.e. "P30D" for 30 days, "PT24H" for 24 hours)'}
                                validator={_queryTimeRangeLimitValidator}
                                required />
            )}
            <TimeRangeOptionsForm options={relativeTimeRangeOptionsUpdate || _buildTimeRangeOptions(config.relative_timerange_options)}
                                  update={this._onRelativeTimeRangeOptionsUpdate}
                                  validator={_relativeTimeRangeValidator}
                                  title="Relative Timerange Options"
                                  help={<span>Configure the available options for the <strong>relative</strong> time range selector as <strong>ISO8601 duration</strong></span>} />
            <TimeRangeOptionsForm options={surroundingTimeRangeOptionsUpdate || _buildTimeRangeOptions(config.surrounding_timerange_options)}
                                  update={this._onSurroundingTimeRangeOptionsUpdate}
                                  validator={_surroundingTimeRangeValidator}
                                  title="Surrounding Timerange Options"
                                  help={<span>Configure the available options for the <strong>surrounding</strong> time range selector as <strong>ISO8601 duration</strong></span>} />

            <Input id="filter-fields-input"
                   type="text"
                   label="Surrounding search filter fields"
                   onChange={this._onFilterFieldsUpdate}
                   value={surroundingFilterFields || filterFieldsString}
                   help="A ',' separated list of message fields that will be used as filter for the surrounding messages query."
                   required />

            <Input id="disabled-fields-input"
                   type="text"
                   label="Disabled analysis fields"
                   onChange={this._onAnalysisDisabledFieldsUpdate}
                   value={analysisDisabledFields || analysisDisabledFieldsString}
                   help="A ',' separated list of message fields for which analysis features like QuickValues will be disabled in the web UI."
                   required />

            <TimeRangeOptionsForm options={autoRefreshOptions}
                                  update={this._onAutoRefreshTimeRangeOptionsUpdate}
                                  validator={_autoRefreshTimeRangeValidator}
                                  title="Auto-Refresh Interval Options"
                                  help={<span>Configure the available options for the <strong>auto-refresh</strong> interval selector as <strong>ISO8601 duration</strong></span>} />
            <Input label="Default Auto-Refresh Option"
                   id="default-auto-refresh-option"
                   required
                   help="Select the interval which is used when auto-refresh is started without explicitly selecting one">
              <Select placeholder="Select the default interval"
                      clearable={false}
                      options={autoRefreshOptions}
                      displayKey="description"
                      valueKey="period"
                      matchProp="label"
                      onChange={this._onAutoRefreshDefaultOptionsUpdate}
                      value={defaultAutoRefreshOption} />
            </Input>
          </fieldset>
        </BootstrapModalForm>
      </div>
    );
  }
}

export default withTelemetry(SearchesConfig);
