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
import isArray from 'lodash/isArray';
import flattenDeep from 'lodash/flattenDeep';
import styled from 'styled-components';

import { KeyboardKey } from 'components/bootstrap';
import { isMacOS as _isMacOS } from 'util/OSUtils';
import SectionComponent from 'components/common/Section/SectionComponent';

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
  height: fit-content;
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
type Props = {
  title: string,
  description: string,
  sectionActions: Array<{
    keyDescription: string,
    reactKey: string,
    keys: string | Array<string>,
    splitKey: string,
    combinationKey: string,
    isEnabled: boolean,
  }>
};

const HotkeyCollectionSection = ({ title, description, sectionActions }: Props) => {
  const isMacOS = _isMacOS();

  return (
    <SectionComponent title={title}>
      <p className="description">{description}</p>
      <ShortcutList>
        {sectionActions.map(({ keyDescription, keys, combinationKey, splitKey, reactKey }) => (
          <Key description={keyDescription}
               keys={keys}
               combinationKey={combinationKey}
               splitKey={splitKey}
               isMacOS={isMacOS}
               key={reactKey} />
        ))}
      </ShortcutList>
    </SectionComponent>
  );
};

export default HotkeyCollectionSection;
