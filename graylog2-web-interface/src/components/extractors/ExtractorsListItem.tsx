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
import React, { useState } from 'react';
import { Trans, useTranslation } from 'react-i18next';
import numeral from 'numeral';
import { useQueryClient } from '@tanstack/react-query';

import { LinkContainer } from 'components/common';
import { Button, Row, Col, Well } from 'components/bootstrap';
import EntityListItem from 'components/common/EntityListItem';
import ExtractorUtils from 'util/ExtractorUtils';
import Routes from 'routing/Routes';
import { EXTRACTORS_QUERY_KEY, deleteExtractor } from 'hooks/useExtractors';

type TimingMetricsProps = {
  timing: any;
};

const TimingMetrics = ({ timing }: TimingMetricsProps) => {
  const { t } = useTranslation('extractors');

  return (
    <dl className="metric-def metric-timer">
      <dt>{t('metrics.percentile_95')}</dt>
      <dd>{t('metrics.microseconds', { value: numeral(timing['95th_percentile']).format('0,0.[00]') })}</dd>

      <dt>{t('metrics.percentile_98')}</dt>
      <dd>{t('metrics.microseconds', { value: numeral(timing['98th_percentile']).format('0,0.[00]') })}</dd>

      <dt>{t('metrics.percentile_99')}</dt>
      <dd>{t('metrics.microseconds', { value: numeral(timing['99th_percentile']).format('0,0.[00]') })}</dd>

      <dt>{t('metrics.std_dev')}</dt>
      <dd>{t('metrics.microseconds', { value: numeral(timing.std_dev).format('0,0.[00]') })}</dd>

      <dt>{t('metrics.mean')}</dt>
      <dd>{t('metrics.microseconds', { value: numeral(timing.mean).format('0,0.[00]') })}</dd>

      <dt>{t('metrics.minimum')}</dt>
      <dd>{t('metrics.microseconds', { value: numeral(timing.min).format('0,0.[00]') })}</dd>

      <dt>{t('metrics.maximum')}</dt>
      <dd>{t('metrics.microseconds', { value: numeral(timing.max).format('0,0.[00]') })}</dd>
    </dl>
  );
};

type MetricsProps = {
  metrics: any;
};

const Metrics = ({ metrics }: MetricsProps) => {
  const { t } = useTranslation('extractors');
  let totalRate;

  if (metrics.total.rate) {
    totalRate = (
      <div className="meter" style={{ marginBottom: 10 }}>
        {t('metrics.invocations', {
          total: numeral(metrics.total.rate.total).format('0,0'),
          oneMinute: numeral(metrics.total.rate.one_minute).format('0,0.[00]'),
          fiveMinutes: numeral(metrics.total.rate.five_minute).format('0,0.[00]'),
          fifteenMinutes: numeral(metrics.total.rate.fifteen_minute).format('0,0.[00]'),
        })}
      </div>
    );
  }

  const conditionCounts = (
    <div className="meter" style={{ marginBottom: 10 }}>
      {t('metrics.hits_misses', { hits: metrics.condition_hits, misses: metrics.condition_misses })}
    </div>
  );

  let totalTime;

  if (metrics.total.time) {
    totalTime = <TimingMetrics timing={metrics.total.time} />;
  } else {
    totalTime = t('metrics.no_messages');
  }

  let conditionTime;

  if (metrics.condition.time) {
    conditionTime = <TimingMetrics timing={metrics.condition.time} />;
  } else {
    conditionTime = t('metrics.no_messages');
  }

  let executionTime;

  if (metrics.execution.time) {
    executionTime = <TimingMetrics timing={metrics.execution.time} />;
  } else {
    executionTime = t('metrics.no_messages');
  }

  let convertersTime;

  if (metrics.converters.time) {
    convertersTime = <TimingMetrics timing={metrics.converters.time} />;
  } else {
    convertersTime = t('metrics.no_messages');
  }

  return (
    <div>
      {totalRate}
      {conditionCounts}
      <Row>
        <Col md={6}>
          <h4 style={{ display: 'inline' }}>{t('metrics.total_time')}</h4>
          <br />
          {totalTime}
        </Col>
        <Col md={6}>
          <h4 style={{ display: 'inline' }}>{t('metrics.condition_time')}</h4>
          <br />
          {conditionTime}
        </Col>
        <Col md={6}>
          <h4 style={{ display: 'inline' }}>{t('metrics.execution_time')}</h4>
          <br />
          {executionTime}
        </Col>
        <Col md={6}>
          <h4 style={{ display: 'inline' }}>{t('metrics.converter_time')}</h4>
          <br />
          {convertersTime}
        </Col>
      </Row>
    </div>
  );
};

type ExtractorsListItemProps = {
  extractor: any;
  inputId: string;
  nodeId: string;
};

const ExtractorsListItem = ({ extractor, inputId, nodeId }: ExtractorsListItemProps) => {
  const { t } = useTranslation('extractors');
  const [showDetails, setShowDetails] = useState(false);
  const queryClient = useQueryClient();

  const _toggleDetails = () => setShowDetails((prev) => !prev);

  const _deleteExtractor = () => {
    // eslint-disable-next-line no-alert
    if (window.confirm(t('list_item.confirm_delete', { title: extractor.title }))) {
      deleteExtractor(inputId, extractor).then(() => queryClient.invalidateQueries({ queryKey: EXTRACTORS_QUERY_KEY }));
    }
  };

  const _formatExtractorSubtitle = () => (
    <span>
      <Trans
        t={t}
        i18nKey={extractor.cursor_strategy === 'cut' ? 'list_item.subtitle_cut' : 'list_item.subtitle_copy'}
        values={{ source: extractor.source_field, target: extractor.target_field }}
        components={{ em: <em /> }}
      />
    </span>
  );

  const _formatCondition = () => {
    if (extractor.condition_type === 'none') {
      return <div />;
    }

    return (
      <div className="configuration-section">
        <h4>{t('list_item.condition')}</h4>
        <ul>
          <li>
            <Trans
              t={t}
              i18nKey={
                extractor.condition_type === 'string' ? 'list_item.condition_string' : 'list_item.condition_regex'
              }
              values={{ value: extractor.condition_value }}
              components={{ em: <em /> }}
            />
          </li>
        </ul>
      </div>
    );
  };

  const _formatActions = () => [
    <Button key={`extractor-details-${extractor.id}`} bsStyle="info" onClick={_toggleDetails}>
      {t('list_item.details')}
    </Button>,
    <LinkContainer
      key={`edit-extractor-${extractor.id}`}
      to={Routes.edit_input_extractor(nodeId, inputId, extractor.id)}>
      <Button>{t('list_item.edit')}</Button>
    </LinkContainer>,
    <Button key="delete-extractor-" bsStyle="danger" onClick={_deleteExtractor}>
      {t('list_item.delete')}
    </Button>,
  ];

  const _formatOptions = (options: { [key: string]: unknown }) => {
    const attributes = Object.keys(options);

    return attributes.map((attribute) => (
      <li key={`${attribute}-${extractor.id}`}>
        {attribute}: {options[attribute] as React.ReactNode}
      </li>
    ));
  };

  const _formatConfiguration = (extractorConfig: { [key: string]: unknown }) => {
    let formattedOptions: React.ReactElement | Array<React.ReactElement> = _formatOptions(extractorConfig);

    if ((formattedOptions as Array<React.ReactElement>).length === 0) {
      formattedOptions = <li>{t('list_item.no_configuration')}</li>;
    }

    return (
      <div className="configuration-section">
        <h4>{t('list_item.configuration')}</h4>
        <ul>{formattedOptions}</ul>
      </div>
    );
  };

  const _formatConverter = (key: string, converter: { type: string; config?: { [key: string]: unknown } }) => (
    <li key={`converter-${key}`}>
      {converter.type}
      {converter.config && <ul>{_formatOptions(converter.config)}</ul>}
    </li>
  );

  const _formatConverters = (converters: { [key: string]: { type: string; config?: { [key: string]: unknown } } }) => {
    const converterKeys = Object.keys(converters);
    const formattedConverters = converterKeys.map((converterKey) =>
      _formatConverter(converterKey, converters[converterKey]),
    );

    if (formattedConverters.length === 0) {
      return <div />;
    }

    return (
      <div className="configuration-section">
        <h4>{t('list_item.converters')}</h4>
        <ul>{formattedConverters}</ul>
      </div>
    );
  };

  const _formatDetails = () => (
    <div>
      <Col md={8}>
        <Well bsSize="small" className="configuration-well">
          {_formatCondition()}
          {_formatConfiguration(extractor.extractor_config)}
          {_formatConverters(extractor.converters)}
        </Well>
      </Col>
      <Col md={4}>
        <div className="graylog-input-metrics">
          <h3>{t('metrics.title')}</h3>
          <Metrics metrics={extractor.metrics} />
        </div>
      </Col>
    </div>
  );

  return (
    <EntityListItem
      key={`entry-list-${extractor.id}`}
      title={extractor.title}
      titleSuffix={ExtractorUtils.getReadableExtractorTypeName(extractor.type)}
      description={_formatExtractorSubtitle()}
      actions={_formatActions()}
      contentRow={showDetails ? _formatDetails() : null}
    />
  );
};

export default ExtractorsListItem;
