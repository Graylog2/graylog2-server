// @flow strict
import React from 'react';
import { render, cleanup, fireEvent, waitForElement } from 'wrappedTestingLibrary';
import { List } from 'immutable';

import Direction from 'views/logic/aggregationbuilder/Direction';
import FieldType from 'views/logic/fieldtypes/FieldType';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import SortConfig from 'views/logic/aggregationbuilder/SortConfig';

import FieldSortSelect from './FieldSortSelect';

describe('FieldSortSelect', () => {
  const properties = ['enumerable'];
  const fieldType1 = new FieldType('string', properties, []);
  const fieldTypeMapping1 = new FieldTypeMapping('date', fieldType1);
  const fieldType2 = new FieldType('string', properties, []);
  const fieldTypeMapping2 = new FieldTypeMapping('http_method', fieldType2);
  const fields = List([fieldTypeMapping1, fieldTypeMapping2]);
  const sort = [new SortConfig('pivot', 'http_method', Direction.Ascending)];

  afterEach(cleanup);

  it('should render minimal', () => {
    const { getByText } = render(<FieldSortSelect fields={fields} onChange={() => {}} sort={sort} />);
    expect(getByText('http_method')).not.toBeNull();
  });

  it('should display current sort as selected option', () => {
    const { getByText } = render(<FieldSortSelect fields={fields} onChange={() => {}} sort={sort} />);
    expect(getByText('http_method')).not.toBeNull();
  });

  it('should open menu when focused', async () => {
    const { getByText, container } = render(<FieldSortSelect fields={fields} onChange={() => {}} sort={sort} />);
    fireEvent.focus(container.getElementsByTagName('input')[0]);
    await waitForElement(() => getByText(/2 results available./));
  });
});
