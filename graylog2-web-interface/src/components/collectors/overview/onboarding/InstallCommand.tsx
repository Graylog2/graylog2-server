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

import { ClipboardButton } from 'components/common';
import { formatDuration } from 'util/ISODurationUtils';

type Props = {
  command: string;
  platformLabel: string;
  tokenDuration: string;
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
    color: ${theme.colors.gray[60]};
    font-size: ${theme.fonts.size.small};
    margin: 0;
  `,
);

const InstallCommand = ({ command, platformLabel, tokenDuration }: Props) => (
  <Container>
    <Header>
      <Title>Run this on {platformLabel}</Title>
      <ClipboardButton text={command} title="Copy command" bsSize="sm" />
    </Header>
    <CommandBlock>{command}</CommandBlock>
    <Note>
      Downloads the collector, enrolls it in the selected fleet, and starts collecting immediately. Token expires in{' '}
      {formatDuration(tokenDuration, () => true)}.
    </Note>
  </Container>
);

export default InstallCommand;
