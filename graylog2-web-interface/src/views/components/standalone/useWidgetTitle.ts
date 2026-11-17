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
import TitleTypes from 'views/stores/TitleTypes';
import defaultTitle from 'views/components/defaultTitle';
import useActiveViewState from 'views/hooks/useActiveViewState';
import type Widget from 'views/logic/widgets/Widget';

const useWidgetTitle = (widget: Widget) => {
  const viewState = useActiveViewState();

  return viewState?.titles?.getIn([TitleTypes.Widget, widget.id], defaultTitle(widget)) as string;
};

export default useWidgetTitle;
