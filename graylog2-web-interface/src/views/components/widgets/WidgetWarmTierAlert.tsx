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
import { useContext, useMemo } from 'react';

import { Alert } from 'components/bootstrap';
import useView from 'views/hooks/useView';
import SearchExplainContext from 'views/components/contexts/SearchExplainContext';

type Props = {
  activeQuery: string
  widgetId: string
}

const WidgetWarmTierAlert = ({ activeQuery, widgetId } : Props) => {
  const { getExplainForWidget } = useContext(SearchExplainContext);
  const view = useView();
  const explainedWidget = getExplainForWidget(activeQuery, widgetId, view.widgetMapping);

  const isWidgetInWarmTier = useMemo(() => explainedWidget?.searched_index_ranges.some((range) => range.is_warm_tiered),
    [explainedWidget?.searched_index_ranges]);

  if (!isWidgetInWarmTier) return null;

  return (
    <Alert bsStyle="info">
      This widget is retrieving data from the Warm Tier and may take longer to load.
    </Alert>
  );
};

export default WidgetWarmTierAlert;
