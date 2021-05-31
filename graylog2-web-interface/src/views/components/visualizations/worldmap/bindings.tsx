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
import type { VisualizationType } from 'views/types';

import Viewport from 'views/logic/aggregationbuilder/visualizations/Viewport';
import WorldMapVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/WorldMapVisualizationConfig';
import WorldMapVisualization from 'views/components/visualizations/worldmap/WorldMapVisualization';

export type WorldMapVisualizationConfigFormValues = {
  zoom: number,
  centerX: number,
  centerY: number,
};

const DEFAULT_FORM_VALUES = {
  zoom: 1,
  centerX: -0.3515602939922709,
  centerY: 0.703125,
};

const worldMap: VisualizationType = {
  type: WorldMapVisualization.type,
  displayName: 'World Map',
  component: WorldMapVisualization,
  config: {
    createConfig: () => DEFAULT_FORM_VALUES,
    fromConfig: (config: WorldMapVisualizationConfig) => {
      if (!config) {
        return DEFAULT_FORM_VALUES;
      }

      return {
        zoom: config.viewport.zoom,
        centerX: config.viewport.center[0],
        centerY: config.viewport.center[1],
      };
    },
    toConfig: (formValues: WorldMapVisualizationConfigFormValues) => WorldMapVisualizationConfig.create(Viewport.create([formValues.centerX, formValues.centerY], formValues.zoom)),
    fields: [
      {
        name: 'zoom',
        title: 'Zoom',
        type: 'numeric',
        required: true,
      },
      {
        name: 'centerX',
        title: 'Latitude',
        type: 'numeric',
        required: true,
        componentProps: {
          step: 'any',
        },
      },
      {
        name: 'centerY',
        title: 'Longitude',
        type: 'numeric',
        required: true,
        componentProps: {
          step: 'any',
        },
      },
    ],
  },

};

export default worldMap;
