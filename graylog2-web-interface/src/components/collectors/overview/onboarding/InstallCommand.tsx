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

import { ClipboardButton } from 'components/common';
import { formatDuration } from 'util/ISODurationUtils';

type Props = {
  command: string;
  platformLabel: string;
  tokenDuration: string | null;
  actions?: React.ReactNode;
  onCopySuccess?: () => void;
};

const Container = styled.div(
  ({ theme }) => css`
    margin-bottom: ${theme.spacings.lg};
  `,
);

const Header = styled.div(
  ({ theme }) => css`
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: ${theme.spacings.sm};
  `,
);

const Title = styled.h3(
  ({ theme }) => css`
    margin: 0;
    font-size: ${theme.fonts.size.h3};
  `,
);

const CommandBlock = styled.pre(
  ({ theme }) => css`
    padding: ${theme.spacings.md};
    background: ${theme.colors.global.contentBackground};
    border: 1px solid ${theme.colors.cards.border};
    border-radius: ${theme.spacings.xs};
    font-family: ${theme.fonts.family.monospace};
    font-size: ${theme.fonts.size.small};
    white-space: pre-wrap;
    word-break: break-all;
    margin-bottom: ${theme.spacings.sm};
  `,
);

const Note = styled.p(
  ({ theme }) => css`
    color: ${theme.colors.text.secondary};
    font-size: ${theme.fonts.size.small};
    margin: 0;
  `,
);

const HeaderActions = styled.div(
  ({ theme }) => css`
    display: flex;
    align-items: center;
    gap: ${theme.spacings.xs};
  `,
);

const InstallCommand = ({
  command,
  platformLabel,
  tokenDuration,
  actions = undefined,
  onCopySuccess = undefined,
}: Props) => {
  const { t } = useTranslation('collectors');

  return (
    <Container>
      <Header>
        <Title>{t('install_command.title', { platform: platformLabel })}</Title>
        <HeaderActions>
          {actions}
          <ClipboardButton text={command} title={t('install_command.copy')} bsSize="sm" onSuccess={onCopySuccess} />
        </HeaderActions>
      </Header>
      <CommandBlock>{command}</CommandBlock>
      <Note>
        {tokenDuration
          ? t('install_command.note_expires', { duration: formatDuration(tokenDuration, () => true) })
          : t('install_command.note_never_expires')}
      </Note>
    </Container>
  );
};

export default InstallCommand;
