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
import { widgetDefinition } from 'views/logic/Widgets';

const defaultTitleGenerator = (w) => `Untitled ${w.type.replace('_', ' ').split(' ').map(_.capitalize).join(' ')}`;

const defaultTitle = (widget) => {
  const widgetDef = widgetDefinition(widget.type);

  return (widgetDef.titleGenerator || defaultTitleGenerator)(widget);
};

export default defaultTitle;
