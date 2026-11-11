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

import WidgetContext from 'views/components/contexts/WidgetContext';
import { AdditionalContext } from 'views/logic/ActionContext';
import WidgetFieldTypesContextProvider from 'views/components/contexts/WidgetFieldTypesContextProvider';
import useWidget from 'views/hooks/useWidget';

type Props = React.PropsWithChildren<{
  widgetId: string;
}>;

/**
 * Provides a widget of the enclosing `StandaloneWidgetGroup` and the contexts widget components need,
 * like the search page does for each of its widgets.
 */
const StandaloneWidgetContextProvider = ({ widgetId, children = undefined }: Props) => {
  const widget = useWidget(widgetId);

  return (
    <WidgetContext.Provider value={widget}>
      {/* Like on the search page, field and value actions get the widget, e.g. for values of several groupings. */}
      <AdditionalContext.Provider value={{ widget }}>
        <WidgetFieldTypesContextProvider>{children}</WidgetFieldTypesContextProvider>
      </AdditionalContext.Provider>
    </WidgetContext.Provider>
  );
};

export default StandaloneWidgetContextProvider;
