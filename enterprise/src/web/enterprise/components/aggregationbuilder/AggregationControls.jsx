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

export default class AggregationControls extends React.Component {
  static propTypes = {
    children: PropTypes.element.isRequired,
    columnPivots: PropTypes.arrayOf(PropTypes.string),
    fields: FieldList.isRequired,
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

    this._onColumnPivotChange = this._onColumnPivotChange.bind(this);
    this._onRowPivotChange = this._onRowPivotChange.bind(this);
    this._onSeriesChange = this._onSeriesChange.bind(this);
    this._onSortChange = this._onSortChange.bind(this);

    const { children, fields, ...rest } = props;
    this.state = rest;
  }

  _onColumnPivotChange(columnPivots) {
    this.setState({ columnPivots: columnPivots.split(',') });
  }

  _onRowPivotChange(rowPivots) {
    this.setState({ rowPivots: rowPivots.split(',') });
  }

  _onSeriesChange(series) {
    this.setState({ series: series.split(',') });
  }

  _onSortChange(sort) {
    this.setState({ sort: sort.split(',') });
  }

  render() {
    const { children, fields } = this.props;
    const formattedFields = fields.map((_, v) => ({ label: v, value: v }))
      .valueSeq()
      .toJS()
      .sort((v1, v2) => naturalSort(v1.label, v2.label));
    return (
      <span>
        <Row>
          <Col md={3} style={{ paddingRight: '2px' }}>
            <VisualizationTypeSelect value={this.state.visualization} onChange={this._onVisualizationChange} />
          </Col>
          <Col md={3} style={{ paddingLeft: '2px', paddingRight: '2px' }}>
            <RowPivotSelect fields={formattedFields} rowPivots={this.state.rowPivots.join(',')} onChange={this._onRowPivotChange} />
          </Col>
          <Col md={3} style={{ paddingLeft: '2px', paddingRight: '2px' }}>
            <ColumnPivotSelect fields={formattedFields} columnPivots={this.state.columnPivots.join(',')} onChange={this._onColumnPivotChange} />
          </Col>
          <Col md={2} style={{ paddingLeft: '2px' }}>
            <SortSelect fields={formattedFields} sort={this.state.sort.join(',')} onChange={this._onSortChange} />
          </Col>
        </Row>
        <Row style={{ height: '100%' }}>
          <Col md={2}>
            <SeriesSelect fields={formattedFields} onChange={this._onSeriesChange} series={this.state.series.join(',')} />
          </Col>
          <Col md={10} style={{ height: '100%' }}>
            {children}
          </Col>
        </Row>
      </span>
    );
  }
}
