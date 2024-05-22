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
import styled, { css } from 'styled-components';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { Col, Row } from 'components/bootstrap';
import { Section, Icon } from 'components/common';
import IndexMaintenanceStrategiesSummary from 'components/indices/IndexMaintenanceStrategiesSummary';
import { DataTieringSummary } from 'components/indices/data-tiering';
import type { IndexSetTemplate } from 'components/indices/IndexSetTemplates/types';

type Props = {
  template: IndexSetTemplate,
}

const FlexWrapper = styled.div(({ theme }) => css`
  display: flex;
  flex-direction: column;
  gap: ${theme.spacings.md};

    dl {
      margin-bottom: 0;
    }

    dl > dd {
      margin-bottom: ${theme.spacings.sm};
    }

    dl > dd:last-child {
      margin-bottom: 0;
    }
`);

const RotationSummaryWrapper = styled.div(({ theme }) => css`
  margin-bottom: ${theme.spacings.sm};
`);

const formatRefreshInterval = (intervalInMs : number) => {
  const intervalInSeconds = intervalInMs / 1000;

  if (intervalInSeconds > 60) {
    return `${intervalInSeconds / 60} minutes`;
  }

  return `${intervalInSeconds} seconds`;
};

const TemplateDetails = ({
  template,
}: Props) => (
  <Row>
    <Col md={12}>
      <FlexWrapper>
        <Section title="Details">
          <dl>
            <dt>Index Analyzer:</dt>
            <dd>{template.index_set_config.index_analyzer}</dd>
            <dt>Shards:</dt>
            <dd>{template.index_set_config.shards}</dd>
            <dt>Replicas:</dt>
            <dd>{template.index_set_config.replicas}</dd>
            <dt>Max. number of segments:</dt>
            <dd>{template.index_set_config.index_optimization_max_num_segments}</dd>
            <dt>Index optimization after rotation:</dt>
            <dd><Icon name={template.index_set_config.index_optimization_disabled ? 'cancel' : 'check_circle'} /></dd>
            <dt>Field type refresh interval:</dt>
            <dd>{formatRefreshInterval(template.index_set_config.field_type_refresh_interval)}</dd>
          </dl>
        </Section>

        <Section title="Rotation & Retention">
          {template.index_set_config.use_legacy_rotation ? (
            <>
              <RotationSummaryWrapper>
                <IndexMaintenanceStrategiesSummary config={{
                  strategy: template.index_set_config.rotation_strategy_class,
                  config: template.index_set_config.rotation_strategy,
                }}
                                                   pluginExports={PluginStore.exports('indexRotationConfig')} />
              </RotationSummaryWrapper>
              <IndexMaintenanceStrategiesSummary config={{
                strategy: template.index_set_config.retention_strategy_class,
                config: template.index_set_config.retention_strategy,
              }}
                                                 rotationStrategyClass={template.index_set_config.rotation_strategy_class}
                                                 pluginExports={PluginStore.exports('indexRetentionConfig')} />
            </>
          ) : (
            <DataTieringSummary config={template.index_set_config.data_tiering} />
          )}
        </Section>
      </FlexWrapper>
    </Col>
  </Row>
);

export default TemplateDetails;
