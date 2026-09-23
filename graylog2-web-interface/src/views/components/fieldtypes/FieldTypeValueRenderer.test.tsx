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
import { render, screen } from 'wrappedTestingLibrary';

import FieldTypeValueRenderer from './FieldTypeValueRenderer';

const rendererFor = (type: string) => FieldTypeValueRenderer.find((r) => r.type === type)!.render;

describe('FieldTypeValueRenderer', () => {
  describe('alert', () => {
    const renderAlert = rendererFor('alert');

    it('shows "Alert"/"Event" as plain text for recognized values', async () => {
      render(<>{renderAlert('true', 'alert', undefined)}</>);
      await screen.findByText('Alert');
    });

    it('shows the raw value for an unrecognized value', async () => {
      // Regression: PlotLegend.tsx assigns the "alert" field to a rollup trace's bare metric name
      // (e.g. "count()") positionally, the same way ChartData.ts does for chart labels — the
      // renderer must show that value as-is rather than force it into "Event".
      render(<>{renderAlert('count()', 'alert', undefined)}</>);
      await screen.findByText('count()');
    });
  });
});
