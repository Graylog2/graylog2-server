// @flow strict
import React, { type AbstractComponent } from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import { SizeMe } from 'react-sizeme';

import type { Rows } from 'views/logic/searchtypes/pivot/PivotHandler';
import type { FieldTypeMappingsList } from 'views/stores/FieldTypesStore';
import type { CurrentViewType } from 'views/components/CustomPropTypes';
import connect from 'stores/connect';
import fieldTypeFor from 'views/logic/fieldtypes/FieldTypeFor';
import Value from 'views/components/Value';
import { ViewStore } from 'views/stores/ViewStore';
import DecoratedValue from 'views/components/messagelist/decoration/DecoratedValue';
import CustomHighlighting from 'views/components/messagelist/CustomHighlighting';
import RenderCompletionCallback from 'views/components/widgets/RenderCompletionCallback';
import NumberVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/NumberVisualizationConfig';
import Trend from './Trend';
import AutoFontSizer from './AutoFontSizer';

type Props = {
  data: Array<{ rows: Rows }>,
  fields: FieldTypeMappingsList,
  config: {
    visualizationConfig: NumberVisualizationConfig,
  },
  currentView: CurrentViewType,
};

const GridContainer: AbstractComponent<{}> = styled.div`
  display: grid;
  grid-template-columns: 1fr;
  grid-template-rows: 4fr 1fr;
  grid-column-gap: 0px;
  grid-row-gap: 0px;
  height: 100%;
  width: 100%;
`;

const SingleItemGrid = styled.div`
  display: grid;
  grid-template-columns: 1fr;
  grid-template-rows: 1fr;
  grid-column-gap: 0px;
  grid-row-gap: 0px;
  height: 100%;
  width: 100%;
`;

const NumberBox = styled.div`
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  width: 100%;
  text-align: center;
  padding-bottom: 10px;
`;

const TrendBox = styled.div`
  height: 100%;
  width: 100%;
`;

class NumberVisualization extends React.Component<Props> {
  _targetRef = undefined;

  static propTypes = {
    data: PropTypes.arrayOf(PropTypes.object).isRequired,
    fields: PropTypes.object.isRequired,
  };


  componentDidMount() {
    const onRenderComplete = this.context;
    if (onRenderComplete) {
      onRenderComplete();
    }
  }

  _extractValueAndField = (rows: Rows) => {
    if (!rows || !rows[0]) {
      return { value: undefined, field: undefined };
    }
    const results = rows[0];
    if (results.source === 'leaf') {
      const leaf = results.values.find(f => f.source === 'row-leaf');
      if (leaf && leaf.source === 'row-leaf') {
        return { value: leaf.value, field: leaf.key[0] };
      }
    }
    return { value: undefined, field: undefined };
  };

  static contextType = RenderCompletionCallback;

  render() {
    const { config: { visualizationConfig = NumberVisualizationConfig.create() }, currentView, fields, data } = this.props;
    const { activeQuery } = currentView;
    const { value, field } = this._extractValueAndField(data[0] ? data[0].rows : []);
    const { value: previousValue } = this._extractValueAndField(data[1] ? data[1].rows : []);
    if (!field || (value !== 0 && !value)) {
      return 'N/A';
    }

    const Container = visualizationConfig.trend ? GridContainer : SingleItemGrid;

    return (
      <Container>
        <NumberBox>
          <SizeMe monitorHeight monitorWidth>
            {({ size }) => (
              <AutoFontSizer height={size.height} width={size.width}>
                <CustomHighlighting field={field} value={value}>
                  <Value field={field}
                         type={fieldTypeFor(field, fields)}
                         value={value}
                         queryId={activeQuery}
                         render={DecoratedValue} />
                </CustomHighlighting>
              </AutoFontSizer>
            )}
          </SizeMe>
        </NumberBox>
        {visualizationConfig.trend && (
          <TrendBox>
            <SizeMe monitorHeight monitorWidth>
              {({ size }) => (
                <AutoFontSizer height={size.height} width={size.width} target={this._targetRef}>
                  <Trend ref={(ref) => { this._targetRef = ref; }}
                         current={value}
                         previous={previousValue}
                         config={visualizationConfig} />
                </AutoFontSizer>
              )}
            </SizeMe>
          </TrendBox>
        )}
      </Container>
    );
  }
}

const ConnectedNumberVisualization = connect(NumberVisualization, { currentView: ViewStore });
ConnectedNumberVisualization.type = 'numeric';

export default ConnectedNumberVisualization;
