import PropTypes from 'prop-types';

import Pivot from 'views/logic/aggregationbuilder/Pivot';
import Series from 'views/logic/aggregationbuilder/Series';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import VisualizationConfig from 'views/logic/aggregationbuilder/visualizations/VisualizationConfig';

import CustomPropTypes from '../CustomPropTypes';

export const FieldList = PropTypes.arrayOf(
  PropTypes.shape({
    label: PropTypes.string.isRequired,
    value: PropTypes.string.isRequired,
  }),
);

export const PivotType = CustomPropTypes.instanceOf(Pivot);
export const PivotList = PropTypes.arrayOf(PivotType);
export const SeriesType = CustomPropTypes.instanceOf(Series);
export const SeriesList = PropTypes.arrayOf(SeriesType);
export const SortList = PropTypes.arrayOf(PropTypes.string);
export const VisualizationType = PropTypes.string;
export const VisualizationConfigType = CustomPropTypes.instanceOf(VisualizationConfig);

export const AggregationType = CustomPropTypes.instanceOf(AggregationWidgetConfig);
export const AggregationResult = PropTypes.objectOf(PropTypes.arrayOf(PropTypes.object));
