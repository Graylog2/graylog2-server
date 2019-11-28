// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import { Col, Row } from 'components/graylog';
import * as Immutable from 'immutable';
import { PluginStore } from 'graylog-web-plugin/plugin';

import CustomPropTypes from 'views/components/CustomPropTypes';
import { defaultCompare } from 'views/logic/DefaultCompare';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';

import VisualizationTypeSelect from './VisualizationTypeSelect';
import ColumnPivotConfiguration from './ColumnPivotConfiguration';
import RowPivotSelect from './RowPivotSelect';
import ColumnPivotSelect from './ColumnPivotSelect';
import SortDirectionSelect from './SortDirectionSelect';
import SortSelect from './SortSelect';
import SeriesSelect from './SeriesSelect';
import DescriptionBox from './DescriptionBox';
import SeriesFunctionsSuggester from './SeriesFunctionsSuggester';
import EventListConfiguration from './EventListConfiguration';

type Props = {
  children: React.Node,
  config: AggregationWidgetConfig,
  fields: Immutable.List<FieldTypeMapping>,
  onChange: (AggregationWidgetConfig) => void,
};

type State = {
  config: AggregationWidgetConfig,
};

const _visualizationConfigFor = (type: string) => PluginStore.exports('visualizationConfigTypes')
  .find(visualizationConfigType => visualizationConfigType && visualizationConfigType.type === type);

export default class AggregationControls extends React.Component<Props, State> {
  static propTypes = {
    children: PropTypes.element.isRequired,
    config: CustomPropTypes.instanceOf(AggregationWidgetConfig).isRequired,
    fields: CustomPropTypes.FieldListType.isRequired,
    onChange: PropTypes.func.isRequired,
  };

  constructor(props: Props) {
    super(props);

    const { config } = props;
    this.state = {
      config,
    };
  }

  _onColumnPivotChange = (columnPivots: $PropertyType<$PropertyType<Props, 'config'>, 'columnPivots'>) => {
    this._setAndPropagate(state => ({ config: state.config.toBuilder().columnPivots(columnPivots).build() }));
  };

  _onRowPivotChange = (rowPivots: $PropertyType<$PropertyType<Props, 'config'>, 'rowPivots'>) => {
    this._setAndPropagate(state => ({ config: state.config.toBuilder().rowPivots(rowPivots).build() }));
  };

  _onSeriesChange = (series: $PropertyType<$PropertyType<Props, 'config'>, 'series'>) => {
    this._setAndPropagate(state => ({ config: state.config.toBuilder().series(series).build() }));
    return true;
  };

  _onSortChange = (sort: $PropertyType<$PropertyType<Props, 'config'>, 'sort'>) => {
    this._setAndPropagate(state => ({ config: state.config.toBuilder().sort(sort).build() }));
  };

  _onSortDirectionChange = (direction: $PropertyType<$PropertyType<Props, 'config'>, 'direction'>) => {
    this._setAndPropagate(state => ({
      config: state.config.toBuilder().sort(state.config.sort
        .map(sort => sort.toBuilder().direction(direction).build())).build(),
    }));
  };

  _onSetEventAnnotation = (value: boolean) => {
    this._setAndPropagate(state => ({ config: state.config.toBuilder().eventAnnotation(value).build() }));
  };

  // eslint-disable-next-line no-undef
  _onRollupChange = (rollup: $PropertyType<$PropertyType<Props, 'config'>, 'rollup'>) => {
    this._setAndPropagate(state => ({ config: state.config.toBuilder().rollup(rollup).build() }));
  };

  _onVisualizationChange = (visualization: $PropertyType<$PropertyType<Props, 'config'>, 'visualization'>) => {
    this._setAndPropagate(state => ({ config: state.config.toBuilder().visualization(visualization).visualizationConfig(undefined).build() }));
  };

  _onVisualizationConfigChange = (visualizationConfig: $PropertyType<$PropertyType<Props, 'config'>, 'visualizationConfig'>) => {
    this._setAndPropagate(state => ({ config: state.config.toBuilder().visualizationConfig(visualizationConfig).build() }));
  };

  _setAndPropagate = (fn: (State) => State) => this.setState(fn, this._propagateState);

  _propagateState() {
    const { config } = this.state;
    const { onChange } = this.props;
    onChange(config);
  }

  render() {
    const { children, fields } = this.props;
    const { config } = this.state;
    const { columnPivots, rowPivots, series, sort, visualization, rollup, visualizationConfig } = config;

    const sortDirection = Immutable.Set(sort.map(s => s.direction)).first();

    const formattedFields = fields
      .map(fieldType => fieldType.name)
      .valueSeq()
      .toJS()
      .sort(defaultCompare);
    const formattedFieldsOptions = formattedFields.map(v => ({ label: v, value: v }));
    const suggester = new SeriesFunctionsSuggester(formattedFields);

    const showEventConfiguration = config.isTimeline && ['bar', 'line', 'scatter'].findIndex(x => x === visualization) >= 0;
    const childrenWithCallback = React.Children.map(children, child => React.cloneElement(child, { onVisualizationConfigChange: this._onVisualizationConfigChange }));
    const VisualizationConfigType = _visualizationConfigFor(visualization);
    return (
      <span>
        <Row>
          <Col md={2} style={{ paddingRight: '2px', paddingLeft: '10px' }}>
            <DescriptionBox description="Visualization Type">
              <VisualizationTypeSelect value={visualization} onChange={this._onVisualizationChange} />
            </DescriptionBox>
          </Col>
          <Col md={3} style={{ paddingLeft: '2px', paddingRight: '2px' }}>
            <DescriptionBox description="Rows" help="Values from these fields generate new rows.">
              <RowPivotSelect fields={formattedFieldsOptions} rowPivots={rowPivots} onChange={this._onRowPivotChange} />
            </DescriptionBox>
          </Col>
          <Col md={3} style={{ paddingLeft: '2px', paddingRight: '2px' }}>
            <DescriptionBox description="Columns"
                            help="Values from these fields generate new subcolumns."
                            configurableOptions={<ColumnPivotConfiguration rollup={rollup} onRollupChange={this._onRollupChange} />}>
              <ColumnPivotSelect fields={formattedFieldsOptions} columnPivots={columnPivots} onChange={this._onColumnPivotChange} />
            </DescriptionBox>
          </Col>
          <Col md={2} style={{ paddingLeft: '2px', paddingRight: '2px' }}>
            <DescriptionBox description="Sorting">
              <SortSelect pivots={rowPivots} series={series} sort={sort} onChange={this._onSortChange} />
            </DescriptionBox>
          </Col>
          <Col md={2} style={{ paddingLeft: '2px', paddingRight: '10px' }}>
            <DescriptionBox description="Direction">
              <SortDirectionSelect disabled={!sort || sort.length === 0}
                                   direction={sortDirection && sortDirection.direction}
                                   onChange={this._onSortDirectionChange} />
            </DescriptionBox>
          </Col>
        </Row>
        <Row style={{ height: 'calc(100% - 110px)' }}>
          <Col md={2} style={{ paddingRight: '2px', paddingLeft: '10px' }}>
            <DescriptionBox description="Metrics" help="The unit which is tracked for every row and subcolumn.">
              <SeriesSelect onChange={this._onSeriesChange} series={series} suggester={suggester} />
            </DescriptionBox>
            {showEventConfiguration && (
              <DescriptionBox description="Event Annotations"
                              help="Configuration to render event annotations to a timebased widget">
                <EventListConfiguration enabled={config.eventAnnotation} onChange={this._onSetEventAnnotation} />
              </DescriptionBox>
            )}
            {VisualizationConfigType && (
              <DescriptionBox description="Visualization config" help="Configuration specifically for the selected visualization type.">
                <VisualizationConfigType.component onChange={this._onVisualizationConfigChange}
                                                   config={visualizationConfig} />
              </DescriptionBox>
            )}
          </Col>
          <Col md={10} style={{ height: '100%', paddingLeft: '7px', marginTop: '5px' }}>
            {childrenWithCallback}
          </Col>
        </Row>
      </span>
    );
  }
}
