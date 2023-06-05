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

import type { NavItemProps } from 'views/components/sidebar/NavItem';
import NavItem from 'views/components/sidebar/NavItem';
import useAppDispatch from 'stores/useAppDispatch';
import { redo } from 'views/logic/slices/undoRedoSlice';
import useAppSelector from 'stores/useAppSelector';
import { selectUndoRedoAvailability } from 'views/logic/slices/undoRedoSelectors';

const RedoNavItem = ({ icon, title, sidebarIsPinned }: NavItemProps) => {
  const dispatch = useAppDispatch();
  const { isRedoAvailable } = useAppSelector(selectUndoRedoAvailability);
  const onClick = useCallback(() => dispatch(redo()), [dispatch]);

  return <NavItem disabled={!isRedoAvailable} onClick={onClick} icon={icon} title={title} sidebarIsPinned={sidebarIsPinned} />;
};

export default RedoNavItem;
