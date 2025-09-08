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

const DataWell = styled.div<{ $color?: 'background' | 'content' }>`
  width: 100%;
  border: 1px solid ${({ theme }) => theme.colors.cards.border};
  border-radius: 8px;
  padding: ${({ theme }) => theme.spacings.md};
  background-color: ${({ theme, $color }) => theme.colors.global[$color || 'background']};
`;

export default DataWell;
