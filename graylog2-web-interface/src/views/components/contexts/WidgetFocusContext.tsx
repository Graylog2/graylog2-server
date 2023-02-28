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

import { singleton } from 'logic/singleton';

export type WidgetFocusingState = {
  id: string,
  editing: false,
  focusing: true,
}

export type WidgetEditingState = {
  id: string,
  editing: true,
  focusing: true,
}

type InitialState = {
  id: undefined,
  editing: false,
  focusing: false,
}

export type FocusContextState = WidgetFocusingState | WidgetEditingState | InitialState;

export type WidgetFocusContextType = {
  focusedWidget: FocusContextState | undefined,
  setWidgetFocusing: (widgetId: string) => void,
  setWidgetEditing: (widgetId: string) => void,
  unsetWidgetFocusing: () => void,
  unsetWidgetEditing: () => void,
};

const defaultContext = {
  focusedWidget: undefined,
  setWidgetFocusing: () => {},
  setWidgetEditing: () => {},
  unsetWidgetFocusing: () => {},
  unsetWidgetEditing: () => {},
};

const WidgetFocus = React.createContext<WidgetFocusContextType>(defaultContext);

export default singleton('contexts.WidgetFocus', () => WidgetFocus);
