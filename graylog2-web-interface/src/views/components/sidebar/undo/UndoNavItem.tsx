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
import { undo } from 'views/logic/slices/undoRedoSlice';
import useAppSelector from 'stores/useAppSelector';
import { selectUndoRedoAvailability } from 'views/logic/slices/undoRedoSelectors';

const UndoNavItem = ({ sidebarIsPinned }: { sidebarIsPinned: boolean }) => {
  const dispatch = useAppDispatch();
  const { isUndoAvailable } = useAppSelector(selectUndoRedoAvailability);
  const onClick = useCallback(() => dispatch(undo()), [dispatch]);

  return <NavItem disabled={!isUndoAvailable} onClick={onClick} icon="undo" title="Undo" sidebarIsPinned={sidebarIsPinned} />;
};

export default UndoNavItem;
