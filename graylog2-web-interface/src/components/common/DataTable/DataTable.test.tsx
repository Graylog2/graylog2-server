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
import cloneDeep from 'lodash/cloneDeep';
import userEvent from '@testing-library/user-event';

import DataTable from 'components/common/DataTable';

const rowFormatter = (row: { title: string }) => (
  <tr>
    <td>{row.title}</td>
  </tr>
);

const simulateTypeAheadFilter = async (filterText: string) => {
  const filterInput = await screen.findByRole('textbox', { name: /filter/i });
  await userEvent.clear(filterInput);
  await userEvent.type(filterInput, filterText);
  await userEvent.click(await screen.findByRole('button', { name: /filter/i }));
};

const rows = [{ title: 'Row 1' }, { title: 'Row 2' }, { title: 'Foo 3' }];
const filterRows = (_rows: typeof rows, filterText: RegExp) => _rows.filter((row) => row.title.match(filterText));

const numberOfRows = () => screen.findAllByRole('cell');

describe('<DataTable />', () => {
  it('should render with no rows', async () => {
    render(<DataTable id="myDataTable" headers={['One']} rows={[]} dataRowFormatter={rowFormatter} />);

    await screen.findByText(/no data available/i);
  });

  it('should render with rows', async () => {
    render(<DataTable id="myDataTable" headers={['One']} rows={rows} dataRowFormatter={rowFormatter} />);

    await screen.findByRole('cell', { name: /row 1/i });
    await screen.findByRole('cell', { name: /row 2/i });
    await screen.findByRole('cell', { name: /foo 3/i });
  });

  it('should update rendered rows when array changes', async () => {
    const { rerender } = render(
      <DataTable id="myDataTable" headers={['One']} rows={rows} dataRowFormatter={rowFormatter} />,
    );

    expect(await numberOfRows()).toHaveLength(rows.length);

    const [, ...nextRows] = rows;

    expect(nextRows).toHaveLength(rows.length - 1);

    rerender(<DataTable id="myDataTable" headers={['One']} rows={nextRows} dataRowFormatter={rowFormatter} />);

    expect(await numberOfRows()).toHaveLength(rows.length - 1);
  });

  it('should filter rows', async () => {
    render(
      <DataTable
        id="myDataTable"
        headers={['One']}
        rows={rows}
        dataRowFormatter={rowFormatter}
        filterKeys={['title']}
      />,
    );

    expect(await numberOfRows()).toHaveLength(rows.length);

    const filteredRows = filterRows(rows, /Row/);

    expect(filteredRows).toHaveLength(rows.length - 1);

    await simulateTypeAheadFilter('Row');

    expect(await numberOfRows()).toHaveLength(rows.length - 1);
  });

  it('should keep filter when row in props change', async () => {
    const { rerender } = render(
      <DataTable
        id="myDataTable"
        headers={['One']}
        rows={rows}
        dataRowFormatter={rowFormatter}
        filterKeys={['title']}
      />,
    );

    expect(await numberOfRows()).toHaveLength(rows.length);

    const filteredRows = filterRows(rows, /Row/);

    await simulateTypeAheadFilter('Row');

    expect(await numberOfRows()).toHaveLength(filteredRows.length);

    const nextRows = rows.concat([{ title: 'Row 4' }]);

    rerender(
      <DataTable
        id="myDataTable"
        headers={['One']}
        rows={nextRows}
        dataRowFormatter={rowFormatter}
        filterKeys={['title']}
      />,
    );

    const nextFilteredRows = filterRows(nextRows, /Row/);

    expect(await numberOfRows()).toHaveLength(nextFilteredRows.length);
  });

  it('should not try to render removed rows', async () => {
    const { rerender } = render(
      <DataTable
        id="myDataTable"
        headers={['One']}
        rows={rows}
        dataRowFormatter={rowFormatter}
        filterKeys={['title']}
      />,
    );

    expect(await numberOfRows()).toHaveLength(rows.length);

    const filteredRows = filterRows(rows, /Row/);

    await simulateTypeAheadFilter('Row');

    expect(await numberOfRows()).toHaveLength(filteredRows.length);

    // Ensure this also works with deep comparison
    const clonedRows = cloneDeep(filteredRows);
    const [, ...nextRows] = clonedRows;

    rerender(
      <DataTable
        id="myDataTable"
        headers={['One']}
        rows={nextRows}
        dataRowFormatter={rowFormatter}
        filterKeys={['title']}
      />,
    );
    const nextFilteredRows = filterRows(nextRows, /Row/);

    expect(await numberOfRows()).toHaveLength(nextFilteredRows.length);
  });
});
