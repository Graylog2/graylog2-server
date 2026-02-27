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

import React, { useCallback, useState } from 'react';

import useHotkey from 'hooks/useHotkey';
import { NavItem } from 'components/bootstrap';
import NavIcon from 'components/navigation/NavIcon';

import QuickJumpModal from './QuickJumpModal';

const QuickJumpModalContainer = () => {
  const [showQuickJumpModal, setShowQuickJumpModal] = useState(false);
  const toggleModal = useCallback(() => setShowQuickJumpModal((cur) => !cur), [setShowQuickJumpModal]);

  useHotkey({
    actionKey: 'show-quick-jump-modal',
    callback: () => setShowQuickJumpModal(true),
    scope: 'general',
  });

  return (
    <>
      <NavItem id="quickjump-search-nav" onClick={toggleModal}>
        <NavIcon type="search" title="Search" />
      </NavItem>
      {showQuickJumpModal && <QuickJumpModal onToggle={() => toggleModal()} />}
    </>
  );
};

export default QuickJumpModalContainer;
