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
import styled from 'styled-components';

import { Button } from 'components/bootstrap';
import { OverlayTrigger, Icon, ExternalLink } from 'components/common';

const QueryHelpButton = styled(Button)`
  padding: 6px 6px;
`;

const ExampleWrapper = styled.div`
  background-color: ${({ theme }) => theme.colors.global.background};
  border-radius: 6px;
  padding: ${({ theme }) => theme.spacings.sm};
  white-space: pre-wrap;
  width: 100%;
  margin: ${({ theme }) => theme.spacings.xs} 0;
  line-height: 1.7;

  & code {
    background-color: transparent;
  }
`;

function Overlay() {
  const getStringVariable = (content: string) => <code>$&#123;{content}&#125;</code>;

  return (
    <div>
      <p>
        Use a template to customize the description shown on the Alerts & Events page when an event is created.
        Templates use{' '}
        {
          <ExternalLink href="https://www.tinymediamanager.org/docs/jmte" target="_blank">
            JMTE
          </ExternalLink>
        }{' '}
        substitution - variables wrapped in {getStringVariable('...')}.
      </p>
      <strong>Event Definition Variables</strong>
      <ExampleWrapper>
        {getStringVariable('event_definition_id')}
        <br />
        {getStringVariable('event_definition_title')}
        <br />
        {getStringVariable('event_definition_type')}
        <br />
        {getStringVariable('event_definition_description')}
      </ExampleWrapper>
      <strong>Custom Fields</strong>
      <p>
        Custom fields defined on the event definition are available under <code>fields</code>.
      </p>
      <ExampleWrapper>{getStringVariable('fields.my_custom_field')}</ExampleWrapper>
      <strong>Source Context</strong>
      <p>
        The <code>source</code> variable contains context from the triggering data. Its contents depend on the event
        definition type:
      </p>
      <p>Filter events: original log message fields.</p>
      <ExampleWrapper>
        {getStringVariable('source.source')} (hostname)
        <br />
        {getStringVariable('source.message')}
        <br />
        {getStringVariable('source.timestamp')}
        <br />
        {getStringVariable('source.some_extracted_field')}
      </ExampleWrapper>
      <p>Aggregation events: group-by values and aggregation results.</p>
      <ExampleWrapper>
        {getStringVariable('source.username')} (group-by field)
        <br />
        {getStringVariable('source.aggregation_value_count_source_bytes_sent')}
        <br />
        {getStringVariable('source.aggregation_key')}
      </ExampleWrapper>
      <strong>Examples</strong>
      <p>Filter on a specific log message:</p>
      <ExampleWrapper>
        Error on {getStringVariable('source.source')}: {getStringVariable('source.message')}
      </ExampleWrapper>
      <p>Aggregation of failed login count per user:</p>
      <ExampleWrapper>
        {getStringVariable('fields.user')} failed to login {getStringVariable('source.aggregation_value_count')} times
      </ExampleWrapper>
    </div>
  );
}

function EventSummaryTemplateHelp() {
  return (
    <OverlayTrigger
      trigger="click"
      rootClose
      placement="right"
      title="Event Summary Template"
      width={500}
      overlay={<Overlay />}>
      <QueryHelpButton bsStyle="link" title="Summary Template Help" aria-label="Show event summary template help">
        <Icon name="help" />
      </QueryHelpButton>
    </OverlayTrigger>
  );
}

export default EventSummaryTemplateHelp;
