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
import { useMemo } from 'react';
import { chunk } from 'lodash';
import { SystemConfiguration } from 'views/types';

import { Row } from 'components/graylog';
import ConfigletContainer from 'pages/configurations/ConfigletContainer';
import CombinedProvider from 'injection/CombinedProvider';

const { ConfigurationsActions } = CombinedProvider.get('Configurations');

const _onUpdate = (configType: string) => (config) => ConfigurationsActions.update(configType, config);

const _getConfig = (configType, configuration) => configuration?.[configType] ?? null;

const _pluginConfigs = (systemConfigs, configuration) => systemConfigs
  .map(({ component: SystemConfigComponent, configType }) => (
    <ConfigletContainer title={configType}>
      <SystemConfigComponent key={`system-configuration-${configType}`}
                             config={_getConfig(configType, configuration) ?? undefined}
                             updateConfig={_onUpdate(configType)} />
    </ConfigletContainer>
  ));

type PluginConfigRowsProps = {
  configuration: Record<string, any>,
  systemConfigs: Array<SystemConfiguration>,
};

const PluginConfigRows = ({ configuration, systemConfigs }: PluginConfigRowsProps) => {
  const pluginConfigs = useMemo(() => _pluginConfigs(systemConfigs, configuration), [configuration, systemConfigs]);

  // Put two plugin config components per row.
  const configRows = chunk(pluginConfigs, 2)
    .map((configChunk, idx) => (
      // eslint-disable-next-line react/no-array-index-key
      <Row key={`plugin-config-row-${idx}`}>
        {configChunk[0]}
        {configChunk[1]}
      </Row>
    ));

  return <>{configRows}</>;
};

export default PluginConfigRows;
