import React from 'react';
import PropTypes from 'prop-types';
import { Col, Row, Checkbox } from 'react-bootstrap';
import naturalSort from 'javascript-natural-sort';

import CustomPropTypes from 'enterprise/components/CustomPropTypes';
import AggregationWidgetConfig from 'enterprise/logic/aggregationbuilder/AggregationWidgetConfig';
import VisualizationTypeSelect from './VisualizationTypeSelect';
import ColumnPivotConfiguration from './ColumnPivotConfiguration';
import RowPivotSelect from './RowPivotSelect';
import ColumnPivotSelect from './ColumnPivotSelect';
import SortSelect from './SortSelect';
import SeriesSelect from './SeriesSelect';
import { PivotList } from './AggregationBuilderPropTypes';
import DescriptionBox from './DescriptionBox';
import SeriesFunctionsSuggester from './SeriesFunctionsSuggester';

export default class AggregationControls extends React.Component {
  static propTypes = {
    children: PropTypes.element.isRequired,
    config: PropTypes.shape({
      columnPivots: PivotList,
      rowPivots: PivotList,
      series: PropTypes.arrayOf(PropTypes.string),
      sort: PropTypes.arrayOf(PropTypes.string),
      visualization: PropTypes.string,
      rollup: PropTypes.bool,
    }).isRequired,
    fields: CustomPropTypes.FieldListType.isRequired,
    onChange: PropTypes.func.isRequired,
  };

  static defaultProps = {
    rowPivots: [],
    columnPivots: [],
    sort: [],
    series: [],
    visualization: 'table',
  };

  constructor(props) {
    super(props);

    const { config } = props;
    const { columnPivots, rowPivots, sort, series, visualization, rollup } = config;
    this.state = { config: new AggregationWidgetConfig(columnPivots, rowPivots, series, sort, visualization, rollup) };
  }

  _onColumnPivotChange = (columnPivots) => {
    this.setState(state => ({ config: state.config.toBuilder().columnPivots(columnPivots).build() }), this._propagateState);
  };

  _onRowPivotChange = (rowPivots) => {
    this.setState(state => ({ config: state.config.toBuilder().rowPivots(rowPivots).build() }), this._propagateState);
  };

  _onSeriesChange = (series) => {
    this.setState(state => ({ config: state.config.toBuilder().series(series).build() }), this._propagateState);
  };

  _onSortChange = (sort) => {
    this.setState(state => ({ config: state.config.toBuilder().sort(sort.split(',')).build() }), this._propagateState);
  };

  _onRollupChange = (checked) => {
    this.setState(state => ({ config: state.config.toBuilder().rollup(checked).build() }), this._propagateState);
  };

  _onVisualizationChange = (visualization) => {
    this.setState(state => ({ config: state.config.toBuilder().visualization(visualization).build() }), this._propagateState);
  };

  _propagateState() {
    this.props.onChange(this.state.config);
  }

  render() {
    const { children, fields } = this.props;
    const { columnPivots, rowPivots, series, sort, visualization, rollup } = this.state.config.toObject();
    const formattedFields = fields
      .map(fieldType => fieldType.name)
      .valueSeq()
      .toJS()
      .sort((v1, v2) => naturalSort(v1, v2));
    const currentlyUsedFields = [].concat(rowPivots, columnPivots, series)
      .sort(naturalSort)
      .map(v => ({ label: v, value: v }));
    const formattedFieldsOptions = formattedFields.map(v => ({ label: v, value: v }));
    const suggester = new SeriesFunctionsSuggester(formattedFields);
    return (
      <span>
        <Row>
          <Col md={3} style={{ paddingRight: '2px' }}>
            <DescriptionBox description="Visualization Type">
              <VisualizationTypeSelect value={visualization} onChange={this._onVisualizationChange} />
            </DescriptionBox>
          </Col>
          <Col md={3} style={{ paddingLeft: '2px', paddingRight: '2px' }}>
            <DescriptionBox description="Row Pivots">
              <RowPivotSelect fields={formattedFieldsOptions} rowPivots={rowPivots} onChange={this._onRowPivotChange} />
            </DescriptionBox>
          </Col>
          <Col md={3} style={{ paddingLeft: '2px', paddingRight: '2px' }}>
            <DescriptionBox description="Column Pivots" configurableOptions={<ColumnPivotConfiguration rollup={rollup} onRollupChange={this._onRollupChange} />}>
              <ColumnPivotSelect fields={formattedFieldsOptions} columnPivots={columnPivots} onChange={this._onColumnPivotChange} />
            </DescriptionBox>
          </Col>
          <Col md={2} style={{ paddingLeft: '2px' }}>
            <DescriptionBox description="Sorting">
              <SortSelect fields={currentlyUsedFields} sort={sort} onChange={this._onSortChange} />
            </DescriptionBox>
          </Col>
        </Row>
        <Row style={{ height: 'calc(100% - 110px)' }}>
          <Col md={2}>
            <DescriptionBox description="Series">
              <SeriesSelect onChange={this._onSeriesChange} series={series} suggester={suggester} />
            </DescriptionBox>
          </Col>
          <Col md={10} style={{ height: '100%' }}>
            {children}
          </Col>
        </Row>
      </span>
    );
  }
}
