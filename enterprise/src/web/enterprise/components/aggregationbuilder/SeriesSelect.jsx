import React from 'react';
import PropTypes from 'prop-types';

import Select from 'components/common/Select';
import { FieldList } from './AggregationBuilderPropTypes';

const parseSeries = (series) => series ? series.split(',') : [];

const SeriesSelect = ({ fields, onChange, series }) => (
  <Select placeholder="Series"
          allowCreate
          onChange={newSeries => onChange(parseSeries(newSeries))}
          options={fields}
          value={series.join(',')}
          multi />
);

SeriesSelect.propTypes = {
  fields: FieldList.isRequired,
  onChange: PropTypes.func.isRequired,
  series: PropTypes.string.isRequired,
};

export default SeriesSelect;
