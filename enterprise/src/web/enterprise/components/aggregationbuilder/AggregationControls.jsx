import React from 'react';
import PropTypes from 'prop-types';
import { Col, Row } from 'react-bootstrap';
import naturalSort from 'javascript-natural-sort';

import VisualizationTypeSelect from './VisualizationTypeSelect';
import RowPivotSelect from './RowPivotSelect';
import ColumnPivotSelect from './ColumnPivotSelect';
import SortSelect from './SortSelect';
import SeriesSelect from './SeriesSelect';
import { FieldList } from './AggregationBuilderPropTypes';
import AggregationWidgetConfig from '../../logic/aggregationbuilder/AggregationWidgetConfig';

export default class AggregationControls extends React.Component {
  static propTypes = {
    children: PropTypes.element.isRequired,
    columnPivots: PropTypes.arrayOf(PropTypes.string),
    fields: FieldList.isRequired,
    onChange: PropTypes.func.isRequired,
    rowPivots: PropTypes.arrayOf(PropTypes.string),
    series: PropTypes.arrayOf(PropTypes.string),
    sort: PropTypes.arrayOf(PropTypes.string),
    visualization: PropTypes.string,
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

    const { columnPivots, rowPivots, sort, series, visualization } = props;
    const config = new AggregationWidgetConfig(columnPivots, rowPivots, series, sort, visualization);
    this.state = { config };
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

  _onVisualizationChange = (visualization) => {
    this.setState(state => ({ config: state.config.toBuilder().visualization(visualization).build() }), this._propagateState);
  };

  _propagateState() {
    this.props.onChange(this.state.config);
  }

  render() {
    const { children, fields } = this.props;
    const { columnPivots, rowPivots, series, sort, visualization } = this.state.config.toObject();
    const formattedFields = fields
      .map(fieldType => fieldType.name)
      .map(v => ({ label: v, value: v }))
      .valueSeq()
      .toJS()
      .sort((v1, v2) => naturalSort(v1.label, v2.label));
    const currentlyUsedFields = [].concat(rowPivots, columnPivots, series)
      .sort(naturalSort)
      .map(v => ({ label: v, value: v }));
    return (
      <span>
        <Row>
          <Col md={3} style={{ paddingRight: '2px' }}>
            <VisualizationTypeSelect value={visualization} onChange={this._onVisualizationChange} />
          </Col>
          <Col md={3} style={{ paddingLeft: '2px', paddingRight: '2px' }}>
            <RowPivotSelect fields={formattedFields} rowPivots={rowPivots} onChange={this._onRowPivotChange} />
          </Col>
          <Col md={3} style={{ paddingLeft: '2px', paddingRight: '2px' }}>
            <ColumnPivotSelect fields={formattedFields} columnPivots={columnPivots} onChange={this._onColumnPivotChange} />
          </Col>
          <Col md={2} style={{ paddingLeft: '2px' }}>
            <SortSelect fields={currentlyUsedFields} sort={sort} onChange={this._onSortChange} />
          </Col>
        </Row>
        <Row style={{ height: 'calc(100% - 60px)' }}>
          <Col md={2}>
            <SeriesSelect fields={formattedFields} onChange={this._onSeriesChange} series={series} />
          </Col>
          <Col md={10} style={{ height: '100%' }}>
            {children}
          </Col>
        </Row>
      </span>
    );
  }
}
