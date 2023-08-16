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
import styled, { css, useTheme } from 'styled-components';
import merge from 'lodash/merge';
import { Overlay, RootCloseWrapper } from 'react-overlays';
import { useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react';

import { Popover } from 'components/bootstrap';
import ColorPicker from 'components/common/ColorPicker';
import Plot from 'views/components/visualizations/plotly/AsyncPlot';
import { colors as defaultColors } from 'views/components/visualizations/Colors';
import type ColorMapper from 'views/components/visualizations/ColorMapper';
import { EVENT_COLOR, eventsDisplayName } from 'views/logic/searchtypes/events/EventHandler';
import { ROOT_FONT_SIZE } from 'theme/constants';
import { humanSeparator } from 'views/Constants';
import FieldType from 'views/logic/fieldtypes/FieldType';
import FieldTypesContext from 'views/components/contexts/FieldTypesContext';
import useActiveQueryId from 'views/hooks/useActiveQueryId';
import Value from 'views/components/Value';
import type AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';

import ChartColorContext from './ChartColorContext';
import styles from './GenericPlot.lazy.css';

import InteractiveContext from '../contexts/InteractiveContext';
import RenderCompletionCallback from '../widgets/RenderCompletionCallback';

const StyledPlot = styled(Plot)(({ theme }) => css`
  .hoverlayer .hovertext {
    rect {
      fill: ${theme.colors.global.contentBackground} !important;
      opacity: 0.9 !important;
    }

    .name {
      fill: ${theme.colors.global.textDefault} !important;
    }

    path {
      stroke: ${theme.colors.global.contentBackground} !important;
    }
  }
`);

const StyledPopover = styled(Popover)<{ $top: number, $left: number}>(({ $top, $left }) => css`
  left: ${$left}${'px'}!important;
  top: ${$top}${'px'}!important;
  z-index: 1000;
`);
const ValueContainer = styled.div`
  display: flex;
  flex-direction: column;
`;

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
  originalLabels?: Array<string>,
  line?: ChartMarker,
  marker?: ChartMarker,
  originalName?: string,
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
  layout?: {},
  onZoom?: (from: string, to: string) => boolean,
  setChartColor?: (data: ChartConfig, color: ColorMapper) => ChartColor,
  config: AggregationWidgetConfig,
  labelFields?: (config: Props['config']) => Array<string>,
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

const plotConfig = { displayModeBar: false, doubleClick: false as const, responsive: true };
const columnPivotsToFields = (config: Props['config']) => config?.columnPivots?.flatMap((pivot) => pivot.fields) ?? [];

const GenericPlot = ({ chartData, config, layout, setChartColor, onZoom, getChartColor, labelFields }: Props) => {
  const [legendConfig, setLegendConfig] = useState<LegendConfig>();
  const [valueTargetCoordinates, setValueTargetCoordinates] = useState<{ top: number, left: number }>(null);
  const [valueItems, setValueItems] = useState<Array<{ label: string, field: string, type: FieldType }>>([]);
  const theme = useTheme();
  const activeQuery = useActiveQueryId();
  const { colors, setColor } = useContext(ChartColorContext);
  const interactive = useContext(InteractiveContext);
  const onRenderComplete = useContext(RenderCompletionCallback);
  const fieldTypes = useContext(FieldTypesContext);
  const popoverRef = useRef();

  useEffect(() => {
    styles.use();

    return () => styles.unuse();
  });

  const _onRelayout = (axis: Axis) => {
    if (!axis.autosize && axis['xaxis.range[0]'] && axis['xaxis.range[1]']) {
      const from = axis['xaxis.range[0]'];
      const to = axis['xaxis.range[1]'];

      return onZoom(from, to);
    }

    return true;
  };

  const _onLegendClick = (e: any) => {
    const name = e.node.textContent;
    const target = e.node.querySelector('g.layers');

    if (getChartColor) {
      const color = getChartColor(e.fullData, name);

      setLegendConfig({ name, target, color });
    }

    return false;
  };

  const _onCloseColorPopup = () => setLegendConfig(undefined);
  const _onColorSelect = (name: string, newColor: string) => setColor(name, newColor)
    .then(_onCloseColorPopup);

  const fontSettings = useMemo(() => ({
    color: theme.colors.global.textDefault,
    size: ROOT_FONT_SIZE * theme.fonts.size.small.replace(/rem|em/i, ''),
    family: theme.fonts.family.body,
  }), [theme.colors.global.textDefault, theme.fonts.family.body, theme.fonts.size.small]);
  const defaultLayout = useMemo(() => ({
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
  }), [fontSettings, theme.colors.variant.lightest.default]);
  const plotLayout = useMemo(() => {
    const mergedData = merge({}, defaultLayout, layout);

    mergedData.shapes = mergedData.shapes.map((shape) => ({
      ...shape,
      line: { color: shape?.line?.color || colors.get(eventsDisplayName, EVENT_COLOR) },
    }));

    return mergedData;
  }, [colors, defaultLayout, layout]);

  const newChartData = useMemo(() => chartData.map((chart) => {
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
  }), [chartData, colors, setChartColor, theme.colors.global.textDefault]);

  const _onCloseValuePopup = useCallback(() => {
    // setTimeout(() => setValueTargetCoordinates(null), 400);
    setValueTargetCoordinates(null);
  }, []);
  const _labelFields = useMemo(() => labelFields(config), [config, labelFields]);
  const onPLotCLick = useCallback(({ event: { clientX, clientY }, points }) => {
    setValueTargetCoordinates({ top: clientY, left: clientX });
    const { label: value } = points[0];

    const labelsWithField = value.split(humanSeparator).map((label: string, idx: number) => {
      const field = _labelFields[idx];
      const fieldType = fieldTypes?.queryFields?.get(activeQuery)?.find((type) => type.name === field)?.type ?? FieldType.Unknown;

      return { label, field, type: fieldType };
    });

    setValueItems(labelsWithField);
  }, [_labelFields, activeQuery, fieldTypes?.queryFields]);

  console.log({ popoverRef });

  return (
    <>
      <StyledPlot data={newChartData}
                  useResizeHandler
                  layout={interactive ? plotLayout : merge({}, nonInteractiveLayout, plotLayout)}
                  style={style}
                  onAfterPlot={onRenderComplete}
                  onClick={onPLotCLick}
                  onLegendClick={interactive ? _onLegendClick : () => false}
                  onRelayout={interactive ? _onRelayout : () => false}
                  config={plotConfig} />
      {legendConfig && (
        <RootCloseWrapper event="mousedown"
                          onRootClose={_onCloseColorPopup}>
          <Overlay show
                   placement="top"
                   target={legendConfig.target}>
            <Popover id="legend-config"
                     title={`Configuration for ${legendConfig.name}`}
                     className={styles.locals.customPopover}
                     data-event-element="Generic Plot">
              <ColorPicker color={legendConfig.color}
                           colors={defaultColors}
                           onChange={(newColor) => _onColorSelect(legendConfig.name, newColor)} />
            </Popover>
          </Overlay>
        </RootCloseWrapper>
      )}
      {valueTargetCoordinates && (
        <Overlay show={valueTargetCoordinates}
                 rootClose
                 onHide={(e) => {
                   console.log(e);
                   _onCloseValuePopup();
                 }}>
          <StyledPopover id="value-config-popover"
                         className={styles.locals.customPopover}
                         data-event-element="Generic Plot"
                         $top={valueTargetCoordinates?.top}
                         $left={valueTargetCoordinates?.left}>
            <ValueContainer ref={popoverRef}> {
              valueItems.map(({ label, field, type }) => (field
                ? <Value key={`${field}:${label}`} type={type} value={label} field={field} interactiveActionCallback={_onCloseValuePopup} />
                : label))
}
            </ValueContainer>
          </StyledPopover>
        </Overlay>
      )}
    </>
  );
};

GenericPlot.defaultProps = {
  layout: {},
  onZoom: () => true,
  getChartColor: undefined,
  setChartColor: undefined,
  labelFields: columnPivotsToFields,
};

export default GenericPlot;
