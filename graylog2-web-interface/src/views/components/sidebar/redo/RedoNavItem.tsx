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
import React, { useCallback } from 'react';

import NavItem from 'views/components/sidebar/NavItem';
import useAppDispatch from 'stores/useAppDispatch';
import useAppSelector from 'stores/useAppSelector';
import { selectUndoRedoAvailability } from 'views/logic/slices/undoRedoSelectors';
import { redo } from 'views/logic/slices/undoRedoActions';
import useHotkey from 'hooks/useHotkey';
import useViewType from 'views/hooks/useViewType';
import type { ViewType } from 'views/logic/views/View';

const TITLE = 'Redo';

const RedoNavItem = ({ sidebarIsPinned }: { sidebarIsPinned: boolean }) => {
  const viewType = useViewType();
  const dispatch = useAppDispatch();
  const { isRedoAvailable } = useAppSelector(selectUndoRedoAvailability);
  const onClick = useCallback(() => dispatch(redo()), [dispatch]);

  useHotkey({
    actionKey: 'redo',
    callback: () => dispatch(redo()),
    scope: viewType.toLowerCase() as Lowercase<ViewType>,
  });

  return (
    <NavItem disabled={!isRedoAvailable}
             onClick={onClick}
             icon="redo"
             title={TITLE}
             ariaLabel={TITLE}
             sidebarIsPinned={sidebarIsPinned} />
  );
};

export default RedoNavItem;
