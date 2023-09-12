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
import styled from 'styled-components';

import { Modal, Label } from 'components/bootstrap';
import useHotkeysContext from 'hooks/useHotkeysContext';
import useHotkeys from 'hooks/useHotkeys';
import type { ScopeName, HotkeyCollection } from 'contexts/HotkeysContext';
import SectionComponent from 'components/common/Section/SectionComponent';
import { LinkButton } from 'components/common';
import SectionGrid from 'components/common/Section/SectionGrid';

const StyledKeysList = styled.div`
  display: inline-flex;
  gap: 5px;
`;

const StyledGrid = styled.div`
  display: grid;
  grid-template-columns: auto auto;
  grid-template-rows: auto;
  align-items: center;
  gap: 10px;
`;

const Content = styled.div`
  padding: 20px;
`;

const keyMapper = (key: string) => {
  const keyMap = {
    mod: 'command',
  };

  return keyMap[key] || key;
};

const Key = ({ description, keys, combinationKey, isEnabled }: { description: string, keys: string, combinationKey: string, isEnabled: boolean}) => {
  const keysArray = keys.split(combinationKey);

  return (
    <>
      <b>{description}:</b>
      <StyledKeysList>{keysArray.map((key, index) => {
        const isLast = index === keysArray.length - 1;

        return (
          <>
            <Label bsStyle={isEnabled ? 'info' : 'default'}>{keyMapper(key)}</Label>
            {!isLast && <span>+</span>}
          </>
        );
      })}
      </StyledKeysList>
    </>
  );
};

const HotkeyCollectionSection = ({ collection, scope }: { collection: HotkeyCollection, scope: ScopeName }) => {
  const { title, description, actions } = collection;
  const { activeHotkeys } = useHotkeysContext();
  const filtratedActions = Object.entries(actions).filter(([actionKey]) => activeHotkeys.has(`${scope}.${actionKey}`),
    // && (activeHotkeys.get(`${scope}.${actionKey}`)?.options?.enabled !== false);
  );
  if (!filtratedActions.length) return null;

  return (
    <SectionComponent title={title}>
      <p className="description">{description}</p>
      <StyledGrid>
        {
        filtratedActions.map(([actionKey, { description: keyDescription, keys }]) => {
          const isEnabled = (activeHotkeys.get(`${scope}.${actionKey}`)?.options?.enabled !== false);

          return (
            <Key description={keyDescription}
                 keys={keys}
                 combinationKey="+"
                 isEnabled={isEnabled} />
          );
        })
      }
      </StyledGrid>
    </SectionComponent>
  );
};

const HotkeysModal = () => {
  const [show, setShow] = useState(false);

  const onHide = useCallback(() => setShow(false), []);
  const onToggle = useCallback(() => setShow((cur) => !cur), []);
  const { hotKeysCollection } = useHotkeysContext();
  useHotkeys('SHOW_HELPER', onToggle, { scopes: 'view' });

  return show && (
    <Modal show={show}
           onHide={onHide}
           bsSize="large">
      <Modal.Header closeButton>
        <Modal.Title>Hotkeys</Modal.Title>
      </Modal.Header>

      <Modal.Body>
        <Content>
          <SectionGrid>
            {
          Object.entries(hotKeysCollection).map(([scope, collection]: [ScopeName, HotkeyCollection]) => (
            <HotkeyCollectionSection scope={scope} collection={collection} />
          ))
          }
          </SectionGrid>
        </Content>
      </Modal.Body>
      <Modal.Footer>
        <LinkButton bsStyle="primary" to="/" target="_blank">Show all hot keys</LinkButton>
      </Modal.Footer>
    </Modal>
  );
};

export default HotkeysModal;
