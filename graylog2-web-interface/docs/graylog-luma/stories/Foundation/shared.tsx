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
import styled from 'styled-components';

export const Token = styled.code`
  font-family: ${({ theme }) => theme.fonts.family.monospace};
  font-size: ${({ theme }) => theme.fonts.size.small};
  color: ${({ theme }) => theme.colors.text.secondary};
  background: rgba(128, 128, 128, 0.12);
  padding: 1px ${({ theme }) => theme.spacings.xs};
  border-radius: 3px;
`;

export const PxLabel = styled.span`
  font-family: ${({ theme }) => theme.fonts.family.monospace};
  font-size: ${({ theme }) => theme.fonts.size.small};
  color: ${({ theme }) => theme.colors.text.secondary};
`;

export const UsageNote = styled.p`
  font-size: ${({ theme }) => theme.fonts.size.small};
  color: ${({ theme }) => theme.colors.text.secondary};
  margin: ${({ theme }) => theme.spacings.xxs} 0 0;
  line-height: 1.5;
`;

export const FoundationItem = styled.div`
  &:not(:last-child) {
    margin-bottom: ${({ theme }) => theme.spacings.md};
    border-bottom: 1px solid rgba(128, 128, 128, 0.15);
    padding-bottom: ${({ theme }) => theme.spacings.md};
  }
`;

export const StoryContainer = styled.div`
  padding-bottom: ${({ theme }) => theme.spacings.xl};
`;

export const TokenRow = styled.div`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacings.sm};
  margin-bottom: ${({ theme }) => theme.spacings.xs};
`;
