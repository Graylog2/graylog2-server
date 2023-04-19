/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import * as React from 'react';
import PropTypes from 'prop-types';
import type { DefaultTheme } from 'styled-components';
import { withTheme } from 'styled-components';
import merge from 'lodash/merge';
import { Overlay, RootCloseWrapper } from 'react-overlays';

import { Popover } from 'components/bootstrap';
import ColorPicker from 'components/common/ColorPicker';
import Plot from 'views/components/visualizations/plotly/AsyncPlot';
import { colors as defaultColors } from 'views/components/visualizations/Colors';
import type ColorMapper from 'views/components/visualizations/ColorMapper';
import { EVENT_COLOR, eventsDisplayName } from 'views/logic/searchtypes/events/EventHandler';
import { ROOT_FONT_SIZE } from 'theme/constants';

import ChartColorContext from './ChartColorContext';
import styles from './GenericPlot.lazy.css';

import InteractiveContext from '../contexts/InteractiveContext';
import RenderCompletionCallback from '../widgets/RenderCompletionCallback';

type LegendConfig = {
  name: string,
  target: HTMLElement,
  color?: string,
};

type ChartMarker = {
  colors?: Array<string>,
  color?: string,
  size?: number,
};

export type ChartConfig = {
  name: string,
  labels: Array<string>,
  line?: ChartMarker,
  marker?: ChartMarker,
};

export type ColorMap = {
  [key: string]: string,
};

export type ChartColor = {
  line?: ChartMarker,
  marker?: ChartMarker,
  outsidetextfont?: {
    color: string,
  },
};

type Props = {
  chartData: Array<any>,
  getChartColor?: (data: Array<ChartConfig>, name: string) => string | undefined | null,
  layout: {},
  onZoom: (from: string, to: string) => boolean,
  setChartColor?: (data: ChartConfig, color: ColorMapper) => ChartColor,
};

type GenericPlotProps = Props & { theme: DefaultTheme };

type State = {
  legendConfig?: LegendConfig,
};

type Axis = {
  autosize: boolean,
};

const nonInteractiveLayout = {
  yaxis: { fixedrange: true },
  xaxis: { fixedrange: true },
  hovermode: false,
};

const style = { height: '100%', width: '100%' };

const config = { displayModeBar: false, doubleClick: false as const, responsive: true };

class GenericPlot extends React.Component<GenericPlotProps, State> {
  static propTypes = {
    chartData: PropTypes.array.isRequired,
    layout: PropTypes.object,
    onZoom: PropTypes.func,
    getChartColor: PropTypes.func,
    setChartColor: PropTypes.func,
  };

  static defaultProps = {
    layout: {},
    onZoom: () => true,
    getChartColor: undefined,
    setChartColor: undefined,
  };

  constructor(props: GenericPlotProps) {
    super(props);
    this.state = {};
  }

  componentDidMount() {
    styles.use();
  }

  componentWillUnmount() {
    styles.unuse();
  }

  _onRelayout = (axis: Axis) => {
    if (!axis.autosize && axis['xaxis.range[0]'] && axis['xaxis.range[1]']) {
      const { onZoom } = this.props;
      const from = axis['xaxis.range[0]'];
      const to = axis['xaxis.range[1]'];

      return onZoom(from, to);
    }

    return true;
  };

  _onLegendClick = (e: any) => {
    const name = e.node.textContent;
    const target = e.node.querySelector('g.layers');
    const { getChartColor } = this.props;

    if (getChartColor) {
      const color = getChartColor(e.fullData, name);

      this.setState({ legendConfig: { name, target, color } });
    }

    return false;
  };

  _onColorSelect = (setColor: (name: string, color: string) => Promise<unknown>, name: string, newColor: string) => setColor(name, newColor)
    .then(this._onCloseColorPopup);

  _onCloseColorPopup = () => this.setState({ legendConfig: undefined });

  render() {
    const { chartData, layout, setChartColor, theme } = this.props;
    const fontSettings = {
      color: theme.colors.global.textDefault,
      size: ROOT_FONT_SIZE * theme.fonts.size.small.replace(/rem|em/i, ''),
      family: theme.fonts.family.body,
    };
    const defaultLayout = {
      shapes: [],
      autosize: true,
      showlegend: true,
      margin: {
        t: 10,
        l: 40,
        r: 10,
        b: 0,
        pad: 0,
      },
      legend: {
        orientation: 'h' as const,
        font: fontSettings,
      },
      hoverlabel: {
        namelength: -1,
      },
      paper_bgcolor: 'transparent',
      plot_bgcolor: 'transparent',
      title: {
        font: fontSettings,
      },
      yaxis: {
        automargin: true,
        gridcolor: theme.colors.variant.lightest.default,
        tickfont: fontSettings,
        title: {
          font: fontSettings,
        },
      },
      xaxis: {
        automargin: true,
        tickfont: fontSettings,
        title: {
          font: fontSettings,
        },
      },
    };
    const plotLayout = merge({}, defaultLayout, layout);

    const { legendConfig } = this.state;

    return (
      <ChartColorContext.Consumer>
        {({ colors, setColor }) => {
          plotLayout.shapes = plotLayout.shapes.map((shape) => ({
            ...shape,
            line: { color: shape?.line?.color || colors.get(eventsDisplayName, EVENT_COLOR) },
          }));

          const newChartData = chartData.map((chart) => {
            if (setChartColor && colors) {
              const conf = setChartColor(chart, colors);

              if (chart.type === 'pie') {
                conf.outsidetextfont = { color: theme.colors.global.textDefault };
              }

              if (chart?.name === eventsDisplayName) {
                const eventColor = colors.get(eventsDisplayName, EVENT_COLOR);

                conf.marker = { color: eventColor, size: 5 };
              }

              if (conf.line || conf.marker) {
                return merge(chart, conf);
              }

              return chart;
            }

            return chart;
          });

          return (
            <InteractiveContext.Consumer>
              {(interactive) => (
                <RenderCompletionCallback.Consumer>
                  {(onRenderComplete) => (
                    <>
                      <Plot data={newChartData}
                            useResizeHandler
                            layout={interactive ? plotLayout : merge({}, nonInteractiveLayout, plotLayout)}
                            style={style}
                            onAfterPlot={onRenderComplete}
                            onClick={interactive ? null : () => false}
                            onLegendClick={interactive ? this._onLegendClick : () => false}
                            onRelayout={interactive ? this._onRelayout : () => false}
                            config={config} />
                      {legendConfig && (
                        <RootCloseWrapper event="mousedown"
                                          onRootClose={this._onCloseColorPopup}>
                          <Overlay show
                                   placement="top"
                                   target={legendConfig.target}>
                            <Popover id="legend-config"
                                     title={`Configuration for ${legendConfig.name}`}
                                     className={styles.locals.customPopover}
                                     data-event-element="Generic Plot">
                              <ColorPicker color={legendConfig.color}
                                           colors={defaultColors}
                                           onChange={(newColor) => this._onColorSelect(setColor, legendConfig.name, newColor)} />
                            </Popover>
                          </Overlay>
                        </RootCloseWrapper>
                      )}
                    </>
                  )}
                </RenderCompletionCallback.Consumer>
              )}
            </InteractiveContext.Consumer>
          );
        }}
      </ChartColorContext.Consumer>
    );
  }
}

export default withTheme(GenericPlot);
