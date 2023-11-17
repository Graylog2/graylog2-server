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
import pick from 'lodash/pick';
import isArray from 'lodash/isArray';
import flattenDeep from 'lodash/flattenDeep';

import { KeyboardKey, Modal, Button } from 'components/bootstrap';
import useHotkeysContext from 'hooks/useHotkeysContext';
import type { ScopeName, HotkeyCollection } from 'contexts/HotkeysContext';
import SectionComponent from 'components/common/Section/SectionComponent';
import SectionGrid from 'components/common/Section/SectionGrid';
import { isMacOS as _isMacOS } from 'util/OSUtils';

const Content = styled.div`
  padding: 20px;
`;

const Footer = styled.div`
  display: flex;
  justify-content: right;
  align-items: center;
`;

const ShortcutList = styled.div`
  display: flex;
  flex-direction: column;
`;

const ShortcutListItem = styled.div`
  display: flex;
  gap: 5px;
  justify-content: space-between;

  &:not(:last-child) {
    margin-bottom: 3px;
  }
`;

const KeysList = styled.div`
  display: inline-flex;
  gap: 5px;
  justify-content: right;
  flex-wrap: wrap;
`;

const KeySeparator = styled.div`
  display: flex;
  align-items: center;
`;

const keyMapper = (key: string, isMacOS: boolean) => {
  const keyMap = {
    mod: isMacOS ? '⌘' : 'Ctrl',
  };

  return keyMap[key] || key;
};

type KeyProps = {
  combinationKey: string,
  description: string,
  isMacOS: boolean,
  keys: string | Array<string>,
  splitKey: string,
}

type ShortcutKeysProps = {
  keys: string | Array<string>,
  splitKey: string,
  combinationKey: string,
  isMacOS: boolean
}

const ShortcutKeys = ({ keys, splitKey, combinationKey, isMacOS }: ShortcutKeysProps) => {
  const shortcutsArray = isArray(keys) ? keys : [keys];
  const splitShortcutsArray = flattenDeep(shortcutsArray.map((key) => key.split(splitKey)));

  return (
    <>
      {splitShortcutsArray.map((keysStr, keysStrIndex) => {
        const keysArray = keysStr.split(combinationKey);
        const isLastSplit = keysStrIndex === splitShortcutsArray.length - 1;

        return (
          <React.Fragment key={keysStr}>
            {keysArray.map((key, index) => {
              const isLast = index === keysArray.length - 1;

              return (
                <React.Fragment key={key}>
                  <KeyboardKey>{keyMapper(key, isMacOS)}</KeyboardKey>
                  {!isLast && <KeySeparator>{combinationKey}</KeySeparator>}
                </React.Fragment>
              );
            })}
            {!isLastSplit && <KeySeparator>or</KeySeparator>}
          </React.Fragment>
        );
      })}
    </>
  );
};

const Key = ({ description, keys, combinationKey, splitKey, isMacOS }: KeyProps) => (
  <ShortcutListItem>
    {description}
    <KeysList>
      <ShortcutKeys keys={keys} combinationKey={combinationKey} splitKey={splitKey} isMacOS={isMacOS} />
    </KeysList>
  </ShortcutListItem>
);

type HotkeyCollectionSectionProps = {
  collection: HotkeyCollection,
  scope: ScopeName,
  isMacOS: boolean
}

const HotkeyCollectionSection = ({ collection, scope, isMacOS }: HotkeyCollectionSectionProps) => {
  const { activeHotkeys } = useHotkeysContext();
  const { title, description, actions } = collection;
  const filtratedActions = Object.entries(actions).filter(([actionKey]) => {
    const key: `${ScopeName}.${string}` = `${scope}.${actionKey}`;

    return activeHotkeys.has(key) && activeHotkeys.get(key).options.displayInOverview !== false;
  });

  if (!filtratedActions.length) {
    return null;
  }

  return (
    <SectionComponent title={title}>
      <p className="description">{description}</p>
      <ShortcutList>
        {filtratedActions.map(([actionKey, { description: keyDescription, keys, displayKeys }]) => {
          const splitKey = activeHotkeys.get(`${scope}.${actionKey}`)?.options?.splitKey;
          const combinationKey = activeHotkeys.get(`${scope}.${actionKey}`)?.options?.combinationKey;
          const reactKey = isArray(keys) ? keys.join(',') : keys;

          return (
            <Key description={keyDescription}
                 keys={displayKeys ?? keys}
                 combinationKey={combinationKey}
                 splitKey={splitKey}
                 isMacOS={isMacOS}
                 key={reactKey} />
          );
        })}
      </ShortcutList>
    </SectionComponent>
  );
};

const useEnabledCollections = () => {
  const { hotKeysCollections, enabledScopes } = useHotkeysContext();
  const allScopesEnabled = enabledScopes.length === 1 && enabledScopes[0] === '*';
  const collection = allScopesEnabled ? hotKeysCollections : pick(hotKeysCollections, enabledScopes);

  return Object.entries(collection);
};

type Props = {
  onToggle: () => void,
}

const HotkeysModal = ({ onToggle }: Props) => {
  const isMacOS = _isMacOS();
  const enabledCollection = useEnabledCollections();

  return (
    <Modal onHide={onToggle}
           show
           title="Keyboard shortcuts"
           bsSize="large">
      <Modal.Header closeButton>
        <Modal.Title>Keyboard shortcuts</Modal.Title>
      </Modal.Header>

      <Modal.Body>
        <Content>
          <SectionGrid>
            {enabledCollection.map(([scope, collection]: [ScopeName, HotkeyCollection]) => (
              <HotkeyCollectionSection scope={scope} collection={collection} isMacOS={isMacOS} key={scope} />
            ))}
          </SectionGrid>
        </Content>
      </Modal.Body>
      <Modal.Footer>
        <Footer>
          {/* <Link to="/" target="_blank">View all keyboard shortcuts</Link> */}
          <Button onClick={() => onToggle()}>Close</Button>
        </Footer>
      </Modal.Footer>
    </Modal>
  );
};

export default HotkeysModal;
