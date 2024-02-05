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
import React, { useMemo } from 'react';
import styled from 'styled-components';
import pick from 'lodash/pick';
import isArray from 'lodash/isArray';

import { Modal, Button } from 'components/bootstrap';
import useHotkeysContext from 'hooks/useHotkeysContext';
import type { ScopeName, HotkeyCollection } from 'contexts/HotkeysContext';
import SectionGrid from 'components/common/Section/SectionGrid';
import HotkeyCollectionSection from 'components/hotkeys/HotkeyCollectionSection';
import { Link } from 'components/common/router';
import Routes from 'routing/Routes';

const Content = styled.div`
  padding: 20px;
`;

const Footer = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
`;

type ModalHotkeyCollectionSectionProps = {
  collection: HotkeyCollection,
  scope: ScopeName,
}

const ModalHotkeyCollectionSection = ({ collection, scope }: ModalHotkeyCollectionSectionProps) => {
  const { activeHotkeys } = useHotkeysContext();
  const { title, description, actions } = collection;
  const filtratedActions = useMemo(() => Object.entries(actions).filter(([actionKey]) => {
    const key: `${ScopeName}.${string}` = `${scope}.${actionKey}`;

    return activeHotkeys.has(key) && activeHotkeys.get(key).options.displayInOverview !== false;
  }).map(([actionKey, { description: keyDescription, keys, displayKeys }]) => {
    const isEnabled = !!activeHotkeys.get(`${scope}.${actionKey}`)?.options?.enabled;
    const splitKey = activeHotkeys.get(`${scope}.${actionKey}`)?.options?.splitKey;
    const combinationKey = activeHotkeys.get(`${scope}.${actionKey}`)?.options?.combinationKey;
    const reactKey = isArray(keys) ? keys.join(',') : keys;

    return ({
      isEnabled,
      splitKey,
      combinationKey,
      reactKey,
      keyDescription,
      keys: displayKeys ?? keys,
    });
  }), [actions, activeHotkeys, scope]);

  if (!filtratedActions.length) {
    return null;
  }

  return (
    <HotkeyCollectionSection sectionActions={filtratedActions}
                             description={description}
                             title={title} />

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
              <ModalHotkeyCollectionSection scope={scope} collection={collection} key={scope} />
            ))}
          </SectionGrid>
        </Content>
      </Modal.Body>
      <Modal.Footer>
        <Footer>
          <Link to={Routes.KEYBOARD_SHORTCUTS} target="_blank">View all keyboard shortcuts</Link>
          <Button onClick={() => onToggle()}>Close</Button>
        </Footer>
      </Modal.Footer>
    </Modal>
  );
};

export default HotkeysModal;
