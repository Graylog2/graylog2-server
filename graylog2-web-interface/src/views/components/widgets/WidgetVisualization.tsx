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
import { useMemo } from 'react';

import type { WidgetResults } from 'views/types';
import { widgetDefinition } from 'views/logic/Widgets';
import type WidgetConfig from 'views/logic/widgets/WidgetConfig';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import type { FieldTypeMappingsList } from 'views/logic/fieldtypes/types';
import useWidgetResults from 'views/components/useWidgetResults';
import FullSizeContainer from 'views/components/aggregationbuilder/FullSizeContainer';
import type WidgetType from 'views/logic/widgets/Widget';

import LoadingWidget from './LoadingWidget';
import ErrorWidget from './ErrorWidget';

type Props = {
  id: string;
  widget: WidgetType;
  title: string;
  editing: boolean;
  queryId: string;
  fields: FieldTypeMappingsList;
  setLoadingState: (loading: boolean) => void;
  onToggleEdit: () => void;
  onWidgetConfigChange: (newWidgetConfig: WidgetConfig) => Promise<void>;
};

const WidgetVisualization = ({
  title,
  id,
  widget,
  fields,
  queryId,
  editing,
  setLoadingState,
  onToggleEdit,
  onWidgetConfigChange,
}: Props) => {
  const VisComponent = useMemo(() => widgetDefinition(widget.type).visualizationComponent, [widget.type]);
  const { error: errors, widgetData: data } = useWidgetResults(id);

  if (errors && errors.length > 0) {
    return <ErrorWidget errors={errors} />;
  }

  if (data) {
    const { config, filter } = widget;

    return (
      <FullSizeContainer>
        {({ height, width }) => (
          <VisComponent
            config={config}
            data={data as WidgetResults}
            editing={editing}
            fields={fields}
            filter={filter}
            queryId={queryId}
            onConfigChange={onWidgetConfigChange}
            setLoadingState={setLoadingState}
            title={title}
            toggleEdit={onToggleEdit}
            type={widget.type}
            id={id}
            height={height}
            width={width}
          />
        )}
      </FullSizeContainer>
    );
  }

  return (
    <LoadingWidget
      visualization={widget.config instanceof AggregationWidgetConfig ? widget.config.visualization : undefined}
    />
  );
};

export default WidgetVisualization;
