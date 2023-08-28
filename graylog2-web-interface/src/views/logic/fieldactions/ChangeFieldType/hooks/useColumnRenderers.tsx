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
import React, { useMemo } from 'react';
import styled from 'styled-components';

import type { ColumnRenderers } from 'components/common/EntityDataTable';
import type { FieldTypeUsage, TypeHistoryItem } from 'views/logic/fieldactions/ChangeFieldType/types';
import { HoverForHelp, Timestamp } from 'components/common';

const Container = styled.div`
  display: flex;
  gap: 5px;
  flex-wrap: wrap;
`;

export const useColumnRenderers = () => {
  const customColumnRenderers: ColumnRenderers<FieldTypeUsage> = useMemo(() => ({
    attributes: {
      streams: {
        renderCell: (streams: Array<string>) => <Container>{streams.map((stream) => <span>{stream}</span>)}</Container>,
      },
      typeHistory: {
        renderCell: (items: Array<TypeHistoryItem>) => (
          <Container>
            {items.map((item) => (
              <span>
                {item.type}
                <HoverForHelp displayLeftMargin>
                  <Timestamp dateTime={item.timestamp} />
                </HoverForHelp>
              </span>
            ))}
          </Container>
        ),
      },
    },
  }), []);

  return customColumnRenderers;
};

export default useColumnRenderers;
