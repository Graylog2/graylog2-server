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

import type { WelcomeWidgetEntry } from './welcomeWidgetBuilders';

const Grid = styled.div(
  ({ theme }) => css`
    display: grid;
    grid-template-columns: repeat(12, minmax(0, 1fr));
    gap: ${theme.spacings.xs};
  `,
);

const Cell = styled.div<{ $columns: number; $height: string }>(
  ({ $columns, $height }) => css`
    grid-column: span ${$columns};
    height: ${$height};
    min-width: 0;
  `,
);

type Props = {
  entries: Array<WelcomeWidgetEntry>;
  renderWidget: (entry: WelcomeWidgetEntry) => React.ReactNode;
};

/**
 * Lays out welcome-page widgets in a grid of 12 columns, in the order of their entries.
 */
const WelcomeWidgetsGrid = ({ entries, renderWidget }: Props) => (
  <Grid>
    {entries.map((entry) => (
      <Cell key={entry.widget.id} $columns={entry.columns} $height={entry.height}>
        {renderWidget(entry)}
      </Cell>
    ))}
  </Grid>
);

export default WelcomeWidgetsGrid;
