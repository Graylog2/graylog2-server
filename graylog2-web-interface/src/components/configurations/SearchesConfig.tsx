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
import { useEffect, useState } from 'react';
import moment from 'moment';

import { ConfigurationType } from 'components/configurations/ConfigurationTypes';
import { Button, Row, Col, BootstrapModalForm, Input } from 'components/bootstrap';
import { IfPermitted, ISODurationInput } from 'components/common';
import { ConfigurationsActions } from 'stores/configurations/ConfigurationsStore';

import 'moment-duration-format';

import TimeRangeOptionsForm from './TimeRangeOptionsForm';
import TimeRangeOptionsSummary from './TimeRangeOptionsSummary';

const queryTimeRangeLimitValidator = (milliseconds) => {
  return milliseconds >= 1;
};

const relativeTimeRangeValidator = (milliseconds, duration) => {
  return milliseconds >= 1 || duration === 'PT0S';
};

const surroundingTimeRangeValidator = (milliseconds) => {
  return milliseconds >= 1;
};

const splitStringList = (stringList) => {
  return stringList.split(',').map((f) => f.trim()).filter((f) => f.length > 0);
};

type Config = {
  query_time_range_limit: string,
  relative_timerange_options: object,
  surrounding_timerange_options: object,
  surrounding_filter_fields: Array<String>,
  analysis_disabled_fields: Array<String>,
}

const SearchesConfig = () => {
  const [showConfigModal, setShowConfigModal] = useState(false);
  const [config, setConfig] = useState<Config | undefined>(undefined);
  const [limitEnabled, setLimitEnabled] = useState(moment.duration(config?.query_time_range_limit).asMilliseconds() > 0);
  const [relativeTimeRangeOptionsUpdate, setRelativeTimeRangeOptionsUpdate] = useState(undefined);
  const [surroundingTimeRangeOptionsUpdate, setSurroundingTimeRangeOptionsUpdate] = useState(undefined);
  const [surroundingFilterFields, setSurroundingFilterFields] = useState(undefined);
  const [analysisDisabledFields, setAnalysisDisabledFields] = useState(undefined);

  useEffect(() => {
    ConfigurationsActions.list(ConfigurationType.SEARCHES_CLUSTER_CONFIG).then((configData) => {
      setConfig(configData as Config);
    });
  }, []);

  const onUpdate = (field) => {
    return (newOptions) => {
      setConfig({ ...config, [field]: newOptions });
    };
  };

  const onRelativeTimeRangeOptionsUpdate = (data) => {
    setRelativeTimeRangeOptionsUpdate(data);
  };

  const onSurroundingTimeRangeOptionsUpdate = (data) => {
    setSurroundingTimeRangeOptionsUpdate(data);
  };

  const onFilterFieldsUpdate = (e) => {
    setSurroundingFilterFields(e.target.value);
  };

  const onAnalysisDisabledFieldsUpdate = (e) => {
    setAnalysisDisabledFields(e.target.value);
  };

  const onChecked = () => {
    let queryTimeRangeLimit;

    if (limitEnabled) {
      // If currently enabled, disable by setting the limit to 0 seconds.
      queryTimeRangeLimit = 'PT0S';
    } else {
      // If currently not enabled, set a default of 30 days.
      queryTimeRangeLimit = 'P30D';
    }

    setConfig({ ...config, query_time_range_limit: queryTimeRangeLimit });
    setLimitEnabled(!limitEnabled);
  };

  const openModal = () => {
    setShowConfigModal(true);
  };

  const closeModal = () => {
    setShowConfigModal(false);
  };

  const saveConfig = () => {
    const update = { ...config };

    if (relativeTimeRangeOptionsUpdate) {
      update.relative_timerange_options = {};

      relativeTimeRangeOptionsUpdate.forEach((entry) => {
        update.relative_timerange_options[entry.period] = entry.description;
      });

      setRelativeTimeRangeOptionsUpdate(undefined);
    }

    if (surroundingTimeRangeOptionsUpdate) {
      update.surrounding_timerange_options = {};

      surroundingTimeRangeOptionsUpdate.forEach((entry) => {
        update.surrounding_timerange_options[entry.period] = entry.description;
      });

      setSurroundingTimeRangeOptionsUpdate(undefined);
    }

    // Make sure to update filter fields
    if (surroundingFilterFields) {
      update.surrounding_filter_fields = splitStringList(surroundingFilterFields);
      setSurroundingFilterFields(undefined);
    }

    if (analysisDisabledFields) {
      update.analysis_disabled_fields = splitStringList(analysisDisabledFields);
      setAnalysisDisabledFields(undefined);
    }

    ConfigurationsActions.update(ConfigurationType.SEARCHES_CLUSTER_CONFIG, config).then(() => {
      closeModal();
    });
  };

  const buildTimeRangeOptions = (options) => {
    return Object.keys(options).map((key) => {
      return { period: key, description: options[key] };
    });
  };

  if (!config) {
    return null;
  }

  const duration = moment.duration(config.query_time_range_limit);
  const limit = limitEnabled ? `${config.query_time_range_limit} (${duration.humanize()})` : 'disabled';

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
        </Col>
        <Col md={6}>
          <strong>Surrounding time range options</strong>
          <TimeRangeOptionsSummary options={config.surrounding_timerange_options} />
        </Col>
        <Col md={6}>

          <strong>Surrounding search filter fields</strong>
          <ul>
            {config.surrounding_filter_fields && config.surrounding_filter_fields.map((f: string) => <li key={f}>{f}</li>)}
          </ul>

          <strong>UI analysis disabled for fields</strong>
          <ul>
            {config.analysis_disabled_fields && (config.analysis_disabled_fields.map((f: string) => <li key={f}>{f}</li>))}
          </ul>

        </Col>

      </Row>
      <IfPermitted permissions="clusterconfigentry:edit">
        <Button bsStyle="info" bsSize="xs" onClick={openModal}>Edit configuration</Button>
      </IfPermitted>

      {showConfigModal && (
      <BootstrapModalForm show
                          title="Update Search Configuration"
                          onSubmitForm={saveConfig}
                          onCancel={closeModal}
                          submitButtonText="Update configuration">
        <fieldset>
          <label htmlFor="query-limit-checkbox">Relative Timerange Options</label>
          <Input id="query-limit-checkbox"
                 type="checkbox"
                 label="Enable query limit"
                 name="enabled"
                 checked={limitEnabled}
                 onChange={onChecked} />
          {limitEnabled && (
          <ISODurationInput id="query-timerange-limit-field"
                            duration={config.query_time_range_limit}
                            update={onUpdate('query_time_range_limit')}
                            label="Query time range limit (ISO8601 Duration)"
                            help={'The maximum time range for searches. (i.e. "P30D" for 30 days, "PT24H" for 24 hours)'}
                            validator={queryTimeRangeLimitValidator}
                            required />
          )}
          <TimeRangeOptionsForm options={relativeTimeRangeOptionsUpdate || buildTimeRangeOptions(config.relative_timerange_options)}
                                update={onRelativeTimeRangeOptionsUpdate}
                                validator={relativeTimeRangeValidator}
                                title="Relative Timerange Options"
                                help={<span>Configure the available options for the <strong>relative</strong> time range selector as <strong>ISO8601 duration</strong></span>} />
          <TimeRangeOptionsForm options={surroundingTimeRangeOptionsUpdate || buildTimeRangeOptions(config.surrounding_timerange_options)}
                                update={onSurroundingTimeRangeOptionsUpdate}
                                validator={surroundingTimeRangeValidator}
                                title="Surrounding Timerange Options"
                                help={<span>Configure the available options for the <strong>surrounding</strong> time range selector as <strong>ISO8601 duration</strong></span>} />

          <Input id="filter-fields-input"
                 type="text"
                 label="Surrounding search filter fields"
                 onChange={onFilterFieldsUpdate}
                 value={surroundingFilterFields || (config.surrounding_filter_fields && config.surrounding_filter_fields.join(', '))}
                 help="A ',' separated list of message fields that will be used as filter for the surrounding messages query."
                 required />

          <Input id="disabled-fields-input"
                 type="text"
                 label="Disabled analysis fields"
                 onChange={onAnalysisDisabledFieldsUpdate}
                 value={analysisDisabledFields || (config.analysis_disabled_fields && config.analysis_disabled_fields.join(', '))}
                 help="A ',' separated list of message fields for which analysis features like QuickValues will be disabled in the web UI."
                 required />
        </fieldset>
      </BootstrapModalForm>
      )}
    </div>
  );
};

export default SearchesConfig;
