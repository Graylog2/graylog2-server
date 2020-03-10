import React from 'react';
import { mount } from 'wrappedEnzyme';

import DataTable from 'components/common/DataTable';
import TypeAheadDataFilter from 'components/common/TypeAheadDataFilter';

const rowFormatter = row => <tr><td>{row.title}</td></tr>;

const simulateTypeAheadFilter = (wrapper, filterText) => {
  const filter = wrapper.find(TypeAheadDataFilter);
  filter.instance().setState({ filterText: filterText });
  filter.instance().filterData();
};

const filterRows = (rows, filterText) => {
  return rows.filter(row => row.title.match(filterText));
};

describe('<DataTable />', () => {
  const rows = [
    { title: 'Row 1' },
    { title: 'Row 2' },
    { title: 'Foo 3' },
  ];

  it('should render with no rows', () => {
    const wrapper = mount(<DataTable id="myDataTable" headers={['One']} rows={[]} dataRowFormatter={rowFormatter} />);
    expect(wrapper.find('table')).toHaveLength(0);
    expect(wrapper.text()).toBe('No data available.');
  });

  it('should render with rows', () => {
    const wrapper = mount(
      <DataTable id="myDataTable" headers={['One']} rows={rows} dataRowFormatter={rowFormatter} />,
    );
    expect(wrapper.find('tbody tr')).toHaveLength(rows.length);
  });

  it('should update rendered rows when array changes', () => {
    const wrapper = mount(
      <DataTable id="myDataTable" headers={['One']} rows={rows} dataRowFormatter={rowFormatter} />,
    );
    expect(wrapper.find('tbody tr')).toHaveLength(rows.length);

    const [, ...nextRows] = rows;
    expect(nextRows).toHaveLength(rows.length - 1);
    wrapper.setProps({ rows: nextRows });
    expect(wrapper.find('tbody tr')).toHaveLength(rows.length - 1);
  });

  it('should filter rows', () => {
    const wrapper = mount(
      <DataTable id="myDataTable" headers={['One']} rows={rows} dataRowFormatter={rowFormatter} filterKeys={['title']} />,
    );
    expect(wrapper.find('tbody tr')).toHaveLength(rows.length);

    const filteredRows = filterRows(rows, /Row/);
    expect(filteredRows).toHaveLength(rows.length - 1);
    simulateTypeAheadFilter(wrapper, 'Row');
    wrapper.update();
    expect(wrapper.state('filteredRows')).toEqual(filteredRows);
    expect(wrapper.find('tbody tr')).toHaveLength(rows.length - 1);
  });

  it('should keep filter when row in props change', () => {
    const wrapper = mount(
      <DataTable id="myDataTable" headers={['One']} rows={rows} dataRowFormatter={rowFormatter} filterKeys={['title']} />,
    );
    expect(wrapper.find('tbody tr')).toHaveLength(rows.length);
    const filteredRows = filterRows(rows, /Row/);
    simulateTypeAheadFilter(wrapper, 'Row');
    wrapper.update();
    expect(wrapper.state('filteredRows')).toEqual(filteredRows);
    expect(wrapper.find('tbody tr')).toHaveLength(filteredRows.length);

    const nextRows = rows.concat([{ title: 'Row 4' }]);
    wrapper.setProps({ rows: nextRows });
    const nextFilteredRows = filterRows(nextRows, /Row/);
    // Length is the same as before, filtering is done by a children and needs an update
    expect(wrapper.find('tbody tr')).toHaveLength(filteredRows.length);
    wrapper.update();
    expect(wrapper.find('tbody tr')).toHaveLength(nextFilteredRows.length);
  });

  it('should not try to render removed rows', () => {
    const wrapper = mount(
      <DataTable id="myDataTable" headers={['One']} rows={rows} dataRowFormatter={rowFormatter} filterKeys={['title']} />,
    );
    expect(wrapper.find('tbody tr')).toHaveLength(rows.length);
    const filteredRows = filterRows(rows, /Row/);
    simulateTypeAheadFilter(wrapper, 'Row');
    wrapper.update();
    expect(wrapper.state('filteredRows')).toEqual(filteredRows);
    expect(wrapper.find('tbody tr')).toHaveLength(filteredRows.length);

    const [, ...nextRows] = filteredRows;
    wrapper.setProps({ rows: nextRows });
    const nextFilteredRows = filterRows(nextRows, /Row/);
    // Check that we don't render the row we deleted, even if filtering is done by a children and needs an update
    expect(wrapper.find('tbody tr')).toHaveLength(nextFilteredRows.length);
    wrapper.update();
    expect(wrapper.find('tbody tr')).toHaveLength(nextFilteredRows.length);
  });
});
