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
import { Trans, useTranslation } from 'react-i18next';

import { Button } from 'components/bootstrap';
import { OverlayTrigger, Icon, ExternalLink } from 'components/common';

const QueryHelpButton = styled(Button)`
  padding: 6px;
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

const getStringVariable = (content: string) => <code>$&#123;{content}&#125;</code>;

// Ignores the children passed by `<Trans>`, so the variable is always rendered.
const StringVariable = ({ content }: { content: string; children?: React.ReactNode }) => getStringVariable(content);

const JmteLink = ({ children = undefined }: { children?: React.ReactNode }) => (
  <ExternalLink href="https://www.tinymediamanager.org/docs/jmte" target="_blank">
    {children}
  </ExternalLink>
);

function Overlay() {
  const { t } = useTranslation('event_definitions');

  return (
    <div>
      <p>
        <Trans
          t={t}
          i18nKey="summary_template_help.intro"
          components={{ jmteLink: <JmteLink />, variable: <StringVariable content="..." /> }}
        />
      </p>
      <strong>{t('summary_template_help.event_definition_variables')}</strong>
      <ExampleWrapper>
        {getStringVariable('event_definition_id')}
        <br />
        {getStringVariable('event_definition_title')}
        <br />
        {getStringVariable('event_definition_type')}
        <br />
        {getStringVariable('event_definition_description')}
      </ExampleWrapper>
      <strong>{t('summary_template_help.custom_fields')}</strong>
      <p>
        <Trans t={t} i18nKey="summary_template_help.custom_fields_description" components={{ code: <code /> }} />
      </p>
      <ExampleWrapper>{getStringVariable('fields.my_custom_field')}</ExampleWrapper>
      <strong>{t('summary_template_help.source_context')}</strong>
      <p>
        <Trans t={t} i18nKey="summary_template_help.source_context_description" components={{ code: <code /> }} />
      </p>
      <p>{t('summary_template_help.filter_events')}</p>
      <ExampleWrapper>
        {getStringVariable('source.source')} {t('summary_template_help.hostname')}
        <br />
        {getStringVariable('source.message')}
        <br />
        {getStringVariable('source.timestamp')}
        <br />
        {getStringVariable('source.some_extracted_field')}
      </ExampleWrapper>
      <p>{t('summary_template_help.aggregation_events')}</p>
      <ExampleWrapper>
        {getStringVariable('source.username')} {t('summary_template_help.group_by_field')}
        <br />
        {getStringVariable('source.aggregation_value_count_source_bytes_sent')}
        <br />
        {getStringVariable('source.aggregation_key')}
      </ExampleWrapper>
      <strong>{t('summary_template_help.examples')}</strong>
      <p>{t('summary_template_help.filter_example')}</p>
      <ExampleWrapper>
        <Trans
          t={t}
          i18nKey="summary_template_help.filter_example_template"
          components={{
            hostVariable: <StringVariable content="source.source" />,
            messageVariable: <StringVariable content="source.message" />,
          }}
        />
      </ExampleWrapper>
      <p>{t('summary_template_help.aggregation_example')}</p>
      <ExampleWrapper>
        <Trans
          t={t}
          i18nKey="summary_template_help.aggregation_example_template"
          components={{
            userVariable: <StringVariable content="fields.user" />,
            countVariable: <StringVariable content="source.aggregation_value_count" />,
          }}
        />
      </ExampleWrapper>
    </div>
  );
}

function EventSummaryTemplateHelp() {
  const { t } = useTranslation('event_definitions');

  return (
    <OverlayTrigger
      trigger="click"
      rootClose
      placement="right"
      title={t('summary_template_help.title')}
      width={500}
      overlay={<Overlay />}>
      <QueryHelpButton
        bsStyle="link"
        title={t('summary_template_help.button_title')}
        aria-label={t('summary_template_help.button_aria_label')}>
        <Icon name="help" />
      </QueryHelpButton>
    </OverlayTrigger>
  );
}

export default EventSummaryTemplateHelp;
