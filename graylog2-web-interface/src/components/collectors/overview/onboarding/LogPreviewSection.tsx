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
import styled, { css } from 'styled-components';

import { Alert, Label } from 'components/bootstrap';
import { Link, Section, Spinner, Timestamp } from 'components/common';
import StringUtils from 'util/StringUtils';

import PulsingDot from './PulsingDot';
import type { LogPreview } from './useCollectorLogPreview';

type Props = {
  title: string;
  searchUrl: string;
  preview: LogPreview | undefined;
  isLoading: boolean;
  error: Error | null;
  collapsible?: boolean;
};

const MessageRow = styled.div(
  ({ theme }) => css`
    font-family: ${theme.fonts.family.monospace};
    font-size: ${theme.fonts.size.small};
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    padding: ${theme.spacings.xxs} 0;
  `,
);

const RowTimestamp = styled.span(
  ({ theme }) => css`
    color: ${theme.colors.gray[60]};
    margin-right: ${theme.spacings.sm};
  `,
);

const EmptyState = styled.div(
  ({ theme }) => css`
    display: flex;
    align-items: center;
    gap: ${theme.spacings.sm};
    color: ${theme.colors.gray[60]};
    padding: ${theme.spacings.sm} 0;
  `,
);

const PreviewBody = ({ preview, isLoading, error }: Pick<Props, 'preview' | 'isLoading' | 'error'>) => {
  // Last good results win over transient refresh errors.
  if (preview && preview.messages.length > 0) {
    return (
      <>
        {preview.messages.map((message) => (
          <MessageRow key={message.id} title={message.text}>
            <RowTimestamp>
              <Timestamp dateTime={message.timestamp} />
            </RowTimestamp>
            {message.text}
          </MessageRow>
        ))}
      </>
    );
  }

  if (error) {
    return (
      <div aria-live="polite">
        <Alert bsStyle="warning">
          Log preview unavailable &mdash; {StringUtils.truncateWithEllipses(error.message, 120)}
        </Alert>
      </div>
    );
  }

  if (isLoading) {
    return <Spinner text="Loading log preview..." delay={0} />;
  }

  return (
    <EmptyState aria-live="polite">
      <PulsingDot />
      No messages yet &mdash; checking every few seconds
    </EmptyState>
  );
};

const LogPreviewSection = ({ title, searchUrl, preview, isLoading, error, collapsible = false }: Props) => (
  <Section
    title={title}
    collapsible={collapsible}
    defaultClosed={collapsible}
    headerLeftSection={collapsible ? <Label bsStyle="default">{preview ? preview.total : '—'}</Label> : undefined}
    actions={<Link to={searchUrl}>Open in search</Link>}>
    <PreviewBody preview={preview} isLoading={isLoading} error={error} />
  </Section>
);

export default LogPreviewSection;
