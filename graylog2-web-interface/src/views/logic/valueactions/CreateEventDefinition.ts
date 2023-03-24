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
import type { ActionHandlerArguments } from 'views/components/actions/ActionHandler';
import type Widget from 'views/logic/widgets/Widget';
import type { EventDefinitionURLConfig } from 'components/event-definitions/hooks/useEventDefinitionConfigFromUrl';
import { seriesToMetrics } from 'views/components/aggregationwizard/metric/MetricElement';

const aggregationMetricValueHandler = ({ widget, value, field }) => {
  const curSeries = widget.config.series.find((series) => series.function === field);
  const { field: agg_field, function: agg_function } = seriesToMetrics([curSeries])[0];

  return ({
    agg_field,
    agg_function,
    agg_value: value,
  });
};

const aggregationValueHandler = ({ value, field }) => {
  return ({
    search: `${field}:${value}`,
  });
};

const messagesValueHandler = ({ value, field }) => {
  return ({
    search: `${field}:${value}`,
  });
};

const logsValueHandler = ({ value, field }) => {
  return ({
    search: `${field}:${value}`,
  });
};

const getSearchParams = () => {

};

const getHandler = ({ widget, field }: { widget: Widget, field: string }) => {
  if (widget.type === 'AGGREGATION') {
    console.log(seriesToMetrics(widget.config.series));
    const isMetrics = !!widget.config.series.find((series) => series.function === field);

    return isMetrics ? aggregationMetricValueHandler : aggregationValueHandler;
  }

  if (widget.type === 'logs') return logsValueHandler;
  if (widget.type === 'messages') return messagesValueHandler;

  throw new Error('This widget type has incorrect type or no handler');
};

const CreateEventDefinition = (args: ActionHandlerArguments) => {
  console.log(
    args,
  );

  const handler = getHandler({ widget: args.contexts.widget, field: args.field });
  const curQuery = args.contexts.view.search.queries.find((query) => query.id === args.queryId);
  console.log({ curQuery });

  return Promise.resolve();
};

export default CreateEventDefinition;
