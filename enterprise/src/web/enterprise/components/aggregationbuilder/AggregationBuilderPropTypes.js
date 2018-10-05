import PropTypes from 'prop-types';
import Pivot from '../../logic/aggregationbuilder/Pivot';
import Series from 'enterprise/logic/aggregationbuilder/Series';

export const FieldList = PropTypes.arrayOf(
  PropTypes.shape({
    label: PropTypes.string.isRequired,
    value: PropTypes.string.isRequired,
  }),
);

export const PivotType = PropTypes.instanceOf(Pivot);
export const PivotList = PropTypes.arrayOf(PivotType);
export const SeriesType = PropTypes.instanceOf(Series);
export const SeriesList = PropTypes.arrayOf(SeriesType);
export const SortList = PropTypes.arrayOf(PropTypes.string);
export const VisualizationType = PropTypes.string;

export const AggregationType = PropTypes.shape({
  rowPivots: PivotList,
  columnPivots: PivotList,
  series: SeriesList,
  sort: SortList,
  visualization: VisualizationType,
});
