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
import { visT } from 'views/components/visualizations/visualizationTranslation';
import type { VisualizationType } from 'views/types';
import Viewport from 'views/logic/aggregationbuilder/visualizations/Viewport';
import WorldMapVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/WorldMapVisualizationConfig';
import WorldMapVisualization from 'views/components/visualizations/worldmap/WorldMapVisualization';

export type WorldMapVisualizationConfigFormValues = {
  zoom: number;
  centerX: number;
  centerY: number;
};

const DEFAULT_FORM_VALUES = {
  zoom: 1,
  centerX: 0,
  centerY: 0,
};

const worldMap: VisualizationType<typeof WorldMapVisualization.type> = {
  type: WorldMapVisualization.type,
  get displayName() {
    return visT('vis_types.world_map');
  },
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
    toConfig: (formValues: WorldMapVisualizationConfigFormValues) =>
      WorldMapVisualizationConfig.create(Viewport.create([formValues.centerX, formValues.centerY], formValues.zoom)),
    fields: [
      {
        name: 'zoom',
        get title() {
          return visT('vis_worldmap.zoom');
        },
        type: 'numeric',
        required: true,
      },
      {
        name: 'centerX',
        get title() {
          return visT('vis_worldmap.latitude');
        },
        type: 'numeric',
        required: true,
        step: 'any',
      },
      {
        name: 'centerY',
        get title() {
          return visT('vis_worldmap.longitude');
        },
        type: 'numeric',
        required: true,
        step: 'any',
      },
    ],
  },
};

export default worldMap;
