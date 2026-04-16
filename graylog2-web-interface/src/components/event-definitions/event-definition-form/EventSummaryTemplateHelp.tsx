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
import { OverlayTrigger, Icon } from 'components/common';

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
        Use a template to customize the Event Summary shown when an event is created. Templates use Graylog variables
        wrapped in {getStringVariable('...')}.
      </p>
      <p>
        You can insert values from the event, event definition, or custom fields. This uses the same template syntax as
        Graylog email notifications.
      </p>
      <strong>Basic example</strong>
      <ExampleWrapper>
        Failed login detected on {getStringVariable('event.source')}
        <br />
        User: {getStringVariable('event.fields.username')}
        <br />
        Count: {getStringVariable('event.fields.count')}
      </ExampleWrapper>
      <strong>Common Variables</strong>
      <ExampleWrapper>
        Event message: {getStringVariable('event.message')}
        <br />
        Source host: {getStringVariable('event.source')}
        <br />
        Event time: {getStringVariable('event.timestamp')}
        <br />
        Grouping key: {getStringVariable('event.key')}
        <br />
        Priority: {getStringVariable('event.priority')}
        <br />
        Custom event field: {getStringVariable('event.fields.my_custom_field')}
      </ExampleWrapper>
      <strong>Conditional Example</strong>
      <ExampleWrapper>
        {getStringVariable('if event.priority == 3')}
        <br />
        critical event on {getStringVariable('event.source')}
        <br />
        {getStringVariable('else')}
        <br />
        event on {getStringVariable('event.source')}
        <br />
        {getStringVariable('end')}
      </ExampleWrapper>
      <strong>Loop Example (Backlog Messages)</strong>
      <ExampleWrapper>
        {getStringVariable('foreach backlog message')}
        <br />- {getStringVariable('message.source')}: {getStringVariable('message.message')}
        <br />
        {getStringVariable('end')}
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
