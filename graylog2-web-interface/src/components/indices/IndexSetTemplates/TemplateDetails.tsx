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
import { useTranslation } from 'react-i18next';
import styled, { css } from 'styled-components';
import { PluginStore } from 'graylog-web-plugin/plugin';
import type { TFunction } from 'i18next';

import { Col, Row } from 'components/bootstrap';
import { Section, StatusIcon } from 'components/common';
import {
  RotationStrategySummary,
  RetentionStrategySummary,
} from 'components/indices/IndexMaintenanceStrategiesSummary';
import {
  DataTieringSummary,
  DATA_TIERING_TYPE,
  prepareDataTieringInitialValues,
  DataTieringVisualisation,
} from 'components/indices/data-tiering';
import type { IndexSetTemplate } from 'components/indices/IndexSetTemplates/types';
import usePluginEntities from 'hooks/usePluginEntities';

type Props = {
  template: IndexSetTemplate;
  showDescription?: boolean;
};

const FlexWrapper = styled(Col)(
  ({ theme }) => css`
    display: flex;
    flex-direction: column;
    gap: ${theme.spacings.md};

    dl {
      margin-bottom: 0;
    }

    dl > dt {
      width: 240px;
      float: left;
      clear: left;
      margin-right: ${theme.spacings.xs};
    }

    dl > dd {
      margin-bottom: ${theme.spacings.sm};
    }

    dl > dd:last-child {
      margin-bottom: 0;
    }
  `,
);

const RotationSummaryWrapper = styled.div(
  ({ theme }) => css`
    margin-bottom: ${theme.spacings.sm};
  `,
);

const WarmTierSummaryWrapper = styled.div(
  ({ theme }) => css`
    margin-top: ${theme.spacings.sm};
  `,
);

const Grid = styled.div(
  ({ theme }) => css`
    display: grid;
    grid-template-columns: 1fr 1fr;
    grid-auto-rows: 1fr;
    gap: ${theme.spacings.md};
  `,
);

const formatRefreshInterval = (intervalInMs: number, t: TFunction) => {
  const intervalInSeconds = intervalInMs / 1000;

  if (intervalInSeconds > 60) {
    return t('templates.minutes', { count: intervalInSeconds / 60 });
  }

  return t('templates.seconds', { count: intervalInSeconds });
};

const TemplateDetails = ({ template, showDescription = false }: Props) => {
  const { t } = useTranslation('indexsets');
  const dataTieringPlugins = usePluginEntities('dataTiering');
  const dataTieringPlugin = dataTieringPlugins.find((plugin) => plugin.type === DATA_TIERING_TYPE.HOT_WARM);
  const dataTieringConfig = prepareDataTieringInitialValues(template.index_set_config.data_tiering, PluginStore, false);

  return (
    <Row>
      <FlexWrapper md={12}>
        {!template.index_set_config.use_legacy_rotation && (
          <Row>
            <Col md={12}>
              <DataTieringVisualisation
                minDays={dataTieringConfig.index_lifetime_min}
                maxDays={dataTieringConfig.index_lifetime_max}
                minDaysInHot={dataTieringConfig.index_hot_lifetime_min}
                warmTierEnabled={dataTieringConfig.warm_tier_enabled}
                archiveData={dataTieringConfig.archive_before_deletion}
              />
            </Col>
          </Row>
        )}

        {template.description && showDescription && (
          <Row>
            <Col md={12}>
              <Section title={t('templates.description')}>
                <p>{template.description}</p>
              </Section>
            </Col>
          </Row>
        )}
        <Row>
          <Col md={12}>
            <Grid>
              <Section title={t('index_set_form.details')}>
                <dl>
                  <dt>{t('templates.index_analyzer')}</dt>
                  <dd>{template.index_set_config.index_analyzer}</dd>
                  <dt>{t('templates.index_shards')}</dt>
                  <dd>{template.index_set_config.shards}</dd>
                  <dt>{t('templates.index_replica')}</dt>
                  <dd>{template.index_set_config.replicas}</dd>
                  <dt>{t('templates.max_segments')}</dt>
                  <dd>{template.index_set_config.index_optimization_max_num_segments}</dd>
                  <dt>{t('templates.optimization_after_rotation')}</dt>
                  <dd>
                    <StatusIcon active={!template.index_set_config.index_optimization_disabled} />
                  </dd>
                  <dt>{t('templates.field_type_refresh_interval')}</dt>
                  <dd>{formatRefreshInterval(template.index_set_config.field_type_refresh_interval, t)}</dd>
                </dl>
              </Section>
              <Section title={t('index_set_form.rotation_retention')}>
                {template.index_set_config.use_legacy_rotation ? (
                  <>
                    <RotationSummaryWrapper>
                      <RotationStrategySummary
                        config={{
                          strategy: template.index_set_config.rotation_strategy_class,
                          config: template.index_set_config.rotation_strategy,
                        }}
                      />
                    </RotationSummaryWrapper>
                    <RetentionStrategySummary
                      config={{
                        strategy: template.index_set_config.retention_strategy_class,
                        config: template.index_set_config.retention_strategy,
                      }}
                      rotationStrategyClass={template.index_set_config.rotation_strategy_class}
                    />
                  </>
                ) : (
                  <>
                    <DataTieringSummary config={template.index_set_config.data_tiering} />
                    {dataTieringPlugin && (
                      <WarmTierSummaryWrapper>
                        <dataTieringPlugin.TiersSummary config={template.index_set_config.data_tiering} />
                      </WarmTierSummaryWrapper>
                    )}
                  </>
                )}
              </Section>
            </Grid>
          </Col>
        </Row>
      </FlexWrapper>
    </Row>
  );
};

export default TemplateDetails;
