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
import styled, { css } from 'styled-components';

import Value from 'views/components/Value';
import Popover from 'components/common/Popover';
import type { ClickPoint } from 'views/components/visualizations/OnClickPopover/Types';

const ValueBox = styled.span<{ $bgColor?: string }>(
  ({ theme }) => css`
    padding: ${theme.spacings.xxs};
  `,
);

const Container = styled.span(
  ({ theme }) => css`
    display: inline-flex;
    align-items: center;
    gap: ${theme.spacings.xxs};
    font-size: ${theme.fonts.size.tiny};
  `,
);

const HeatmapOnClickPopover = ({ clickPoint }: { clickPoint: ClickPoint }) => {
  if (!clickPoint) return null;

  return (
    <Popover.Dropdown title={String(clickPoint?.x)}>
      <Value
        field={clickPoint.data.name}
        value={clickPoint.y}
        render={() => (
          <Container>
            <ValueBox>{`${String(clickPoint.text ?? clickPoint.y)}`}</ValueBox>
            <span>{clickPoint.data.name}</span>
          </Container>
        )}
      />
    </Popover.Dropdown>
  );
};

export default HeatmapOnClickPopover;
