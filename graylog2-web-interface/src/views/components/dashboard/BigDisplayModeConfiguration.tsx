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
import React, { useCallback, useState, useContext } from 'react';
import { useTranslation } from 'react-i18next';
import URI from 'urijs';

import BootstrapModalForm from 'components/bootstrap/BootstrapModalForm';
import { Checkbox, ControlLabel, FormGroup, HelpBlock, MenuItem } from 'components/bootstrap';
import Input from 'components/bootstrap/Input';
import Routes from 'routing/Routes';
import type View from 'views/logic/views/View';
import queryTitle from 'views/logic/queries/QueryTitle';
import WidgetFocusContext from 'views/components/contexts/WidgetFocusContext';
import type { HistoryFunction } from 'routing/useHistory';
import useHistory from 'routing/useHistory';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

export type UntypedBigDisplayModeQuery = {
  interval?: string;
  refresh?: string;
  tabs?: string;
};

type Configuration = {
  queryCycleInterval?: number;
  queryTabs?: Array<number>;
  refreshInterval: number;
};

type ConfigurationModalProps = {
  onClose: () => void;
  onSave: (configuration: Configuration) => void;
  show: boolean;
  view: View;
};

interface CheckboxTarget {
  target: {
    checked: boolean;
  };
}

const ConfigurationModal = ({ onSave, view, show, onClose }: ConfigurationModalProps) => {
  const { t } = useTranslation('dashboards');
  const availableTabs = view.search.queries
    .keySeq()
    .map((query, idx) => [idx, queryTitle(view, query.id)])
    .toJS();
  const [refreshInterval, setRefreshInterval] = useState(10);
  const [queryTabs, setQueryTabs] = useState(availableTabs.map(([idx]) => idx));
  const [queryCycleInterval, setQueryCycleInterval] = useState(30);
  const addQueryTab = useCallback((idx) => setQueryTabs([...queryTabs, idx]), [queryTabs, setQueryTabs]);
  const removeQueryTab = useCallback(
    (idx) => setQueryTabs(queryTabs.filter((tab) => tab !== idx)),
    [queryTabs, setQueryTabs],
  );
  const _onSave = useCallback(
    () =>
      onSave({
        refreshInterval,
        queryTabs,
        queryCycleInterval,
      }),
    [onSave, refreshInterval, queryTabs, queryCycleInterval],
  );

  return (
    <BootstrapModalForm
      bsSize="large"
      onCancel={onClose}
      onSubmitForm={_onSave}
      submitButtonText={t('full_screen.start')}
      title={t('full_screen.title')}
      show={show}>
      <Input
        autoFocus
        id="refresh-interval"
        type="number"
        min="1"
        name="refresh-interval"
        label={t('full_screen.refresh_interval')}
        help={t('full_screen.refresh_interval_help')}
        onChange={({ target: { value } }) => setRefreshInterval(Number.parseInt(value, 10))}
        required
        step={1}
        value={Number.isNaN(refreshInterval) ? '' : refreshInterval}
      />

      <FormGroup>
        <ControlLabel>{t('full_screen.tabs')}</ControlLabel>
        <ul>
          {availableTabs.map(([idx, title]) => (
            <li key={`${idx}-${title}`}>
              <Checkbox
                inline
                checked={queryTabs.includes(idx)}
                onChange={(event) =>
                  (event as unknown as CheckboxTarget).target.checked ? addQueryTab(idx) : removeQueryTab(idx)
                }>
                {title}
              </Checkbox>
            </li>
          ))}
        </ul>
        <HelpBlock>{t('full_screen.tabs_help')}</HelpBlock>
      </FormGroup>

      <Input
        id="query-cycle-interval"
        type="number"
        min="1"
        name="query-cycle-interval"
        label={t('full_screen.cycle_interval')}
        help={t('full_screen.cycle_interval_help')}
        onChange={({ target: { value } }) => setQueryCycleInterval(Number.parseInt(value, 10))}
        required
        step="1"
        value={Number.isNaN(queryCycleInterval) ? '' : queryCycleInterval}
      />
    </BootstrapModalForm>
  );
};

const redirectToBigDisplayMode = (
  history: HistoryFunction,
  view: View,
  config: UntypedBigDisplayModeQuery,
  unsetWidgetFocusing,
): void => {
  unsetWidgetFocusing();

  history.push(new URI(Routes.DASHBOARD.FULL_SCREEN(view.id)).search(config).toString());
};

const createQueryFromConfiguration = (
  { queryCycleInterval = undefined, queryTabs = undefined, refreshInterval }: Configuration,
  view: View,
): UntypedBigDisplayModeQuery => {
  const basicConfiguration = {
    interval: Number(queryCycleInterval).toString(),
    refresh: Number(refreshInterval).toString(),
  };
  const allQueryIndices = view.search.queries
    .toIndexedSeq()
    .map((_, v) => v)
    .toJS();

  return !queryTabs || allQueryIndices.join(',') === queryTabs.join(',')
    ? basicConfiguration
    : { ...basicConfiguration, tabs: queryTabs.join(',') };
};

type Props = {
  disabled?: boolean;
  show?: boolean;
  view: View;
};

const BigDisplayModeConfiguration = ({ disabled = false, show = false, view }: Props) => {
  const { t } = useTranslation('dashboards');
  const [showConfigurationModal, setShowConfigurationModal] = useState(show);
  const { unsetWidgetFocusing } = useContext(WidgetFocusContext);
  const history = useHistory();
  const sendTelemetry = useSendTelemetry('dashboard-menu');

  const onSave = (config: Configuration) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.DASHBOARD_FULL_SCREEN_MODE_STARTED, {
      app_pathname: 'dashboard',
    });

    redirectToBigDisplayMode(history, view, createQueryFromConfiguration(config, view), unsetWidgetFocusing);
  };

  return (
    <>
      {showConfigurationModal && (
        <ConfigurationModal onClose={() => setShowConfigurationModal(false)} onSave={onSave} show view={view} />
      )}
      <MenuItem disabled={disabled} onSelect={() => setShowConfigurationModal(true)} icon="live_tv">
        {t('full_screen.menu')}
      </MenuItem>
    </>
  );
};

export default BigDisplayModeConfiguration;
