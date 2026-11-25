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
import type { PageToolHandler } from 'components/assistant';
import type { TimeConfigType, ValuesConfigType } from 'views/logic/aggregationbuilder/Pivot';
import type { SeriesConfigJson } from 'views/logic/aggregationbuilder/SeriesConfig';
import type { SortConfigJson } from 'views/logic/aggregationbuilder/SortConfig';
import type { DirectionJson } from 'views/logic/aggregationbuilder/Direction';
import type { Filter, VisualizationType } from 'views/logic/widgets/events/EventsWidgetConfig';
import type { TimeRange } from 'views/logic/queries/Query';

// The arguments of the search page's tools, as the enterprise plugin's page tools send them.
export type VisualizationSettings = Record<string, unknown>;

export type PivotArgs = {
  fields: Array<string>;
  type: string;
  config: TimeConfigType | Pick<ValuesConfigType, 'limit'>;
};

export type SeriesArgs = { function: string; config: Partial<SeriesConfigJson> };

// An aggregation the Assistant gives; the widget keeps its formatting, units and event annotations.
export type AggregationConfigArgs = {
  row_pivots: Array<PivotArgs>;
  column_pivots: Array<PivotArgs>;
  series: Array<SeriesArgs>;
  sort: Array<SortConfigJson>;
  visualization: string;
  rollup: boolean;
  visualization_config?: VisualizationSettings;
};

// Without them, the widget uses the query, streams and time range of the page.
export type WidgetSearchArgs = {
  title: string;
  query?: string;
  streams?: Array<string>;
  timerange?: TimeRange;
};

export type SetQueryArgs = { query: string };

export type SetTimerangeArgs = { timerange: TimeRange };

export type AddWidgetArgs = WidgetSearchArgs & { config: AggregationConfigArgs };

export type UpdateWidgetArgs = Partial<WidgetSearchArgs> & {
  widget_id: string;
  config?: AggregationConfigArgs;
  // The settings of the visualization the config switches to, for those its changes do not give.
  visualization_defaults?: VisualizationSettings;
  // Changed settings of the widget's visualization, which must still be the given one, without changing its aggregation.
  visualization?: string;
  visualization_config?: VisualizationSettings;
};

export type AddMessageTableArgs = WidgetSearchArgs & {
  fields?: Array<string>;
  sort?: { field: string; direction: DirectionJson };
};

export type AddEventsWidgetArgs = WidgetSearchArgs & { mode: VisualizationType; filters: Array<Filter> };

export type AddTextWidgetArgs = { title: string; text: string };

// `asked` tells whether the user asked for the change, or the Assistant suggested it.
export type AddSearchFilterArgs = {
  filter_id: string;
  title: string;
  query_string: string;
  description: string;
  negation: boolean;
  widget_id?: string;
  asked: boolean;
};

export type RemoveSearchFilterArgs = {
  filter_id: string;
  title: string;
  widget_id?: string;
  asked: boolean;
};

export type SearchPageTools = {
  set_query: PageToolHandler<SetQueryArgs>;
  set_timerange: PageToolHandler<SetTimerangeArgs>;
  add_widget: PageToolHandler<AddWidgetArgs>;
  add_message_table: PageToolHandler<AddMessageTableArgs>;
  add_events_widget: PageToolHandler<AddEventsWidgetArgs>;
  add_text_widget: PageToolHandler<AddTextWidgetArgs>;
  update_widget: PageToolHandler<UpdateWidgetArgs>;
  add_search_filter: PageToolHandler<AddSearchFilterArgs>;
  remove_search_filter: PageToolHandler<RemoveSearchFilterArgs>;
};
