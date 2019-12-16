// @flow strict
import * as React from 'react';
import { mount } from 'wrappedEnzyme';

import Series from 'views/logic/aggregationbuilder/Series';
import type { FieldTypeMappingsList } from 'views/stores/FieldTypesStore';
import Pivot from 'views/logic/aggregationbuilder/Pivot';
import { FieldTypes } from 'views/logic/fieldtypes/FieldType';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';

import Headers from './Headers';

jest.mock('components/common/Timestamp', () => 'Timestamp');
jest.mock('logic/datetimes/DateTime', () => 'DateTime');

describe('Headers', () => {
  /* eslint-disable react/require-default-props */
  type RenderHeadersProps = {
    columnPivots?: Array<Pivot>,
    rowPivots?: Array<Pivot>,
    series?: Array<Series>,
    rollup?: boolean,
    actualColumnPivotFields?: Array<Array<string>>,
    fields?: FieldTypeMappingsList,
  };
  /* eslint-enable react/require-default-props */

  const RenderHeaders = ({
    columnPivots = [],
    rowPivots = [],
    series = [],
    rollup = true,
    actualColumnPivotFields = [],
    fields = [],
  }: RenderHeadersProps) => (
    <table>
      <thead>
        <Headers activeQuery="queryId"
                 columnPivots={columnPivots}
                 rowPivots={rowPivots}
                 series={series}
                 rollup={rollup}
                 actualColumnPivotFields={actualColumnPivotFields}
                 fields={fields} />
      </thead>
    </table>
  );

  it('renders a header for every series', () => {
    const wrapper = mount(<RenderHeaders series={[
      Series.forFunction('count()'),
      Series.forFunction('avg(foo)'),
    ]} />);
    expect(wrapper).not.toBeEmptyRender();
    const fields = wrapper.find('Field');
    expect(fields).toHaveLength(2);
  });

  it('passes the correct, inferred type for series', () => {
    const wrapper = mount(<RenderHeaders series={[
      Series.forFunction('count()'),
      Series.forFunction('avg(foo)'),
      Series.forFunction('min(foo)'),
    ]}
                                         fields={[FieldTypeMapping.create('foo', FieldTypes.DATE())]} />);
    expect(wrapper).not.toBeEmptyRender();
    const fields = wrapper.find('Field');

    const countField = fields.at(0);
    expect(countField.props().type).toEqual(FieldTypes.LONG());

    const avgField = fields.at(1);
    expect(avgField.props().type).toEqual(FieldTypes.DATE());

    const minField = fields.at(2);
    expect(minField.props().type).toEqual(FieldTypes.DATE());
  });
});
