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
import { useTranslation } from 'react-i18next';

import { Alert, Label } from 'components/bootstrap';
import { Link, Section, Spinner, Timestamp } from 'components/common';
import StringUtils from 'util/StringUtils';
import type { LogPreview } from 'components/collectors/hooks/useCollectorLogPreview';

import PulsingDot from './PulsingDot';

type Props = {
  title: string;
  /** Omitted when the viewer lacks read permission on the underlying stream. */
  searchUrl?: string;
  preview: LogPreview | undefined;
  isLoading: boolean;
  error: Error | null;
  collapsible?: boolean;
  /** Explains what the preview is showing (and when it refreshes), rendered below the messages. */
  caption?: React.ReactNode;
  onOpenSearch?: () => void;
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
    color: ${theme.colors.text.secondary};
    margin-right: ${theme.spacings.sm};
  `,
);

const EmptyState = styled.div(
  ({ theme }) => css`
    display: flex;
    align-items: center;
    gap: ${theme.spacings.sm};
    color: ${theme.colors.text.secondary};
    padding: ${theme.spacings.sm} 0;
  `,
);

const Caption = styled.div(
  ({ theme }) => css`
    margin-top: ${theme.spacings.sm};
    font-size: ${theme.fonts.size.small};
    color: ${theme.colors.text.secondary};
  `,
);

const PreviewBody = ({ preview, isLoading, error }: Pick<Props, 'preview' | 'isLoading' | 'error'>) => {
  const { t } = useTranslation('collectors');

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
          {t('log_preview.unavailable', { error: StringUtils.truncateWithEllipses(error.message, 120) })}
        </Alert>
      </div>
    );
  }

  if (isLoading) {
    return <Spinner text={t('log_preview.loading')} delay={0} />;
  }

  return (
    <EmptyState aria-live="polite">
      <PulsingDot />
      {t('log_preview.empty')}
    </EmptyState>
  );
};

const LogPreviewSection = ({
  title,
  searchUrl = undefined,
  preview,
  isLoading,
  error,
  collapsible = false,
  caption = undefined,
  onOpenSearch = undefined,
}: Props) => {
  const { t } = useTranslation('collectors');

  return (
    <Section
      title={title}
      collapsible={collapsible}
      defaultClosed={collapsible}
      headerLeftSection={collapsible ? <Label bsStyle="default">{preview ? preview.total : '—'}</Label> : undefined}
      actions={
        searchUrl ? (
          <Link to={searchUrl} onClick={onOpenSearch}>
            {t('deployment.open_in_search')}
          </Link>
        ) : undefined
      }>
      <PreviewBody preview={preview} isLoading={isLoading} error={error} />
      {caption && <Caption>{caption}</Caption>}
    </Section>
  );
};

export default LogPreviewSection;
