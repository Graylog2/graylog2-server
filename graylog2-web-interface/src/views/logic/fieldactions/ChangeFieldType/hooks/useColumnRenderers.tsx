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
import styled, { css } from 'styled-components';
import take from 'lodash/take';
import last from 'lodash/last';

import type { ColumnRenderers } from 'components/common/EntityDataTable';
import type { FieldTypeUsage, TypeHistoryItem } from 'views/logic/fieldactions/ChangeFieldType/types';

const RestTypesContainer = styled.i(({ theme }) => css`
  font-size: ${theme.fonts.size.small};
  display: block;
  margin-top: 5px;
`);

const FlexContainer = styled.div`
  display: inline-flex;
  gap: 5px;
  flex-wrap: wrap;
`;

export const useColumnRenderers = () => {
  const customColumnRenderers: ColumnRenderers<FieldTypeUsage> = useMemo(() => ({
    attributes: {
      streamTitles: {
        renderCell: (streams: Array<string>) => <FlexContainer>{streams.map((stream) => <span>{stream}</span>)}</FlexContainer>,
      },
      types: {
        renderCell: (items: Array<TypeHistoryItem>) => {
          const latest = last(items);
          const rest = take(items, items.length - 1);

          return (
            <div>
              <span><b>{latest}</b></span>
              {!!rest.length && (
              <RestTypesContainer>
                (previous values:
                <FlexContainer>
                  {rest.map((item) => <span>{item}</span>)}
                </FlexContainer>
                )
              </RestTypesContainer>
              )}
            </div>
          );
        },
      },
    },
  }), []);

  return customColumnRenderers;
};

export default useColumnRenderers;
