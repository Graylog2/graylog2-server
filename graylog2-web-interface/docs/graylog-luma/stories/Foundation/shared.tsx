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
  padding: 1px 6px;
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
  margin: 4px 0 0;
  line-height: 1.5;
`;

export const Strong = styled.strong`
  color: ${({ theme }) => theme.colors.text.primary};
`;

export const PageTitle = styled.h1`
  font-family: ${({ theme }) => theme.fonts.family.navigation};
  font-size: ${({ theme }) => theme.fonts.size.h1};
  color: ${({ theme }) => theme.colors.text.primary};
  margin: 0 0 24px;
`;

export const FoundationItem = styled.div`
  padding: 16px 0;

  & + & {
    border-top: 1px solid rgba(128, 128, 128, 0.15);
  }
`;

export const TokenRow = styled.div`
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 6px;
`;
