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

const TimingMetrics = ({ timing }: TimingMetricsProps) => (
  <dl className="metric-def metric-timer">
    <dt>95th percentile:</dt>
    <dd>{numeral(timing['95th_percentile']).format('0,0.[00]')}&#956;s</dd>

    <dt>98th percentile:</dt>
    <dd>{numeral(timing['98th_percentile']).format('0,0.[00]')}&#956;s</dd>

    <dt>99th percentile:</dt>
    <dd>{numeral(timing['99th_percentile']).format('0,0.[00]')}&#956;s</dd>

    <dt>Standard deviation:</dt>
    <dd>{numeral(timing.std_dev).format('0,0.[00]')}&#956;s</dd>

    <dt>Mean:</dt>
    <dd>{numeral(timing.mean).format('0,0.[00]')}&#956;s</dd>

    <dt>Minimum:</dt>
    <dd>{numeral(timing.min).format('0,0.[00]')}&#956;s</dd>

    <dt>Maximum:</dt>
    <dd>{numeral(timing.max).format('0,0.[00]')}&#956;s</dd>
  </dl>
);

type MetricsProps = {
  metrics: any;
};

const Metrics = ({ metrics }: MetricsProps) => {
  let totalRate;

  if (metrics.total.rate) {
    totalRate = (
      <div className="meter" style={{ marginBottom: 10 }}>
        {numeral(metrics.total.rate.total).format('0,0')} total invocations since boot, averages:{' '}
        {numeral(metrics.total.rate.one_minute).format('0,0.[00]')},{' '}
        {numeral(metrics.total.rate.five_minute).format('0,0.[00]')},{' '}
        {numeral(metrics.total.rate.fifteen_minute).format('0,0.[00]')}.
      </div>
    );
  }

  const conditionCounts = (
    <div className="meter" style={{ marginBottom: 10 }}>
      {metrics.condition_hits} hits, {metrics.condition_misses} misses
    </div>
  );

  let totalTime;

  if (metrics.total.time) {
    totalTime = <TimingMetrics timing={metrics.total.time} />;
  } else {
    totalTime = 'No message passed through here yet.';
  }

  let conditionTime;

  if (metrics.condition.time) {
    conditionTime = <TimingMetrics timing={metrics.condition.time} />;
  } else {
    conditionTime = 'No message passed through here yet.';
  }

  let executionTime;

  if (metrics.execution.time) {
    executionTime = <TimingMetrics timing={metrics.execution.time} />;
  } else {
    executionTime = 'No message passed through here yet.';
  }

  let convertersTime;

  if (metrics.converters.time) {
    convertersTime = <TimingMetrics timing={metrics.converters.time} />;
  } else {
    convertersTime = 'No message passed through here yet.';
  }

  return (
    <div>
      {totalRate}
      {conditionCounts}
      <Row>
        <Col md={6}>
          <h4 style={{ display: 'inline' }}>Total time</h4>
          <br />
          {totalTime}
        </Col>
        <Col md={6}>
          <h4 style={{ display: 'inline' }}>Condition time</h4>
          <br />
          {conditionTime}
        </Col>
        <Col md={6}>
          <h4 style={{ display: 'inline' }}>Execution time</h4>
          <br />
          {executionTime}
        </Col>
        <Col md={6}>
          <h4 style={{ display: 'inline' }}>Converter time</h4>
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
  const [showDetails, setShowDetails] = useState(false);
  const queryClient = useQueryClient();

  const _toggleDetails = () => setShowDetails((prev) => !prev);

  const _deleteExtractor = () => {
    // eslint-disable-next-line no-alert
    if (window.confirm(`Really remove extractor "${extractor.title}?"`)) {
      deleteExtractor(inputId, extractor).then(() => queryClient.invalidateQueries({ queryKey: EXTRACTORS_QUERY_KEY }));
    }
  };

  const _formatExtractorSubtitle = () => (
    <span>
      Trying to extract data from <em>{extractor.source_field}</em> into <em>{extractor.target_field}</em>,{' '}
      {extractor.cursor_strategy === 'cut' && 'not'} leaving the original intact.
    </span>
  );

  const _formatCondition = () => {
    if (extractor.condition_type === 'none') {
      return <div />;
    }

    return (
      <div className="configuration-section">
        <h4>Condition</h4>
        <ul>
          <li>
            Will only attempt to run if the message{' '}
            {extractor.condition_type === 'string' ? 'includes the string' : 'matches the regular expression'}{' '}
            <em>{extractor.condition_value}</em>
          </li>
        </ul>
      </div>
    );
  };

  const _formatActions = () => [
    <Button key={`extractor-details-${extractor.id}`} bsStyle="info" onClick={_toggleDetails}>
      Details
    </Button>,
    <LinkContainer
      key={`edit-extractor-${extractor.id}`}
      to={Routes.edit_input_extractor(nodeId, inputId, extractor.id)}>
      <Button>Edit</Button>
    </LinkContainer>,
    <Button key="delete-extractor-" bsStyle="danger" onClick={_deleteExtractor}>
      Delete
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
      formattedOptions = <li>No configuration options</li>;
    }

    return (
      <div className="configuration-section">
        <h4>Configuration</h4>
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
        <h4>Converters</h4>
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
          <h3>Metrics</h3>
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
