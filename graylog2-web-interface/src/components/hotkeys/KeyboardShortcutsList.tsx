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
import React from 'react';
import styled from 'styled-components';

import useHotkeysContext from 'hooks/useHotkeysContext';
import SectionGrid from 'components/common/Section/SectionGrid';
import type { HotkeyCollection, ScopeName } from 'contexts/HotkeysContext';
import HotkeyCollectionSection from 'components/hotkeys/HotkeyCollectionSection';
import { DEFAULT_COMBINATION_KEY, DEFAULT_SPLIT_KEY } from 'hooks/useHotkey';

const StyledSectionGrid = styled(SectionGrid)`
  margin-top: 10px;
`;

const KeyboardShortcutsList = () => {
  const { hotKeysCollections } = useHotkeysContext();

  return (
    <div>
      <StyledSectionGrid>
        {Object.entries(hotKeysCollections).map(([scope, collection]: [ScopeName, HotkeyCollection]) => {
          const { title, description, actions } = collection;
          const sectionActions = Object.entries(actions).map(([actionKey, { description: keyDescription, keys, displayKeys }]) => ({
            isEnabled: true,
            splitKey: DEFAULT_SPLIT_KEY,
            combinationKey: DEFAULT_COMBINATION_KEY,
            reactKey: actionKey,
            keyDescription,
            keys: displayKeys ?? keys,
          }));

          return <HotkeyCollectionSection key={scope} title={title} description={description} sectionActions={sectionActions} />;
        })}
      </StyledSectionGrid>
    </div>
  );
};

export default KeyboardShortcutsList;
