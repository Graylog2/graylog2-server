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

import * as React from 'react';
import { useEffect, useCallback } from 'react';
import styled from 'styled-components';

import { defaultCompare as naturalSort } from 'logic/DefaultCompare';
import Entity from 'logic/content-packs/Entity';
import { ExpandableList, Icon, ExpandableCheckboxListItem } from 'components/common';
import { Input } from 'components/bootstrap';

import style from './ContentPackSelection.css';

const HeaderText = styled.span`
  overflow-wrap: anywhere;
`;

const HeaderIcon = styled(Icon)(
  ({ theme }) => `
  padding-right: ${theme.spacings.xxs};
`,
);

const _entityItemHeader = (entity) => {
  if (entity instanceof Entity) {
    return (
      <>
        <HeaderIcon name="archive" className={style.contentPackEntity} /> <span>{entity.title}</span>
      </>
    );
  }

  return (
    <>
      <HeaderIcon name="dns" /> <HeaderText>{entity.title}</HeaderText>
    </>
  );
};

const toDisplayTitle = (title) => {
  const newTitle = title.split('_').join(' ');

  return newTitle[0].toUpperCase() + newTitle.substr(1);
};

const ContentPackSelectionList = ({
  isFiltered,
  entities,
  selectedEntities,
  isGroupSelected,
  updateSelectionGroup,
  updateSelectionEntity,
}: {
  isFiltered: boolean;
  entities: { [key: string]: Array<{ title: string; id: string }> };
  selectedEntities: { [key: string]: Array<{ id: string }> };
  isGroupSelected: (group: unknown) => boolean;
  updateSelectionGroup: (group: unknown) => void;
  updateSelectionEntity: (entity: unknown) => void;
}) => {
  const [expandedSections, setExpandedSections] = React.useState<Array<string>>(
    isFiltered ? Object.keys(entities) : [],
  );

  useEffect(() => {
    setExpandedSections(isFiltered ? Object.keys(entities) : []);
  }, [isFiltered, entities]);

  const isSelected = useCallback(
    (entity) => {
      const typeName = entity.type.name;

      if (!selectedEntities[typeName]) {
        return false;
      }

      return selectedEntities[typeName].findIndex((e) => e.id === entity.id) >= 0;
    },
    [selectedEntities],
  );

  const _isUndetermined = useCallback(
    (type) => {
      if (!selectedEntities[type]) {
        return false;
      }

      return !(selectedEntities[type].length === entities[type].length || selectedEntities[type].length === 0);
    },
    [entities, selectedEntities],
  );

  return (
    <ExpandableList
      value={expandedSections}
      onChange={(newExpandedSections) => setExpandedSections(newExpandedSections)}>
      {Object.keys(entities)
        .sort((a, b) => naturalSort(a, b))
        .map((entityType) => {
          const group = entities[entityType];

          if (group.length <= 0) {
            return null;
          }

          return (
            <ExpandableCheckboxListItem
              key={entityType}
              value={entityType}
              onChange={() => updateSelectionGroup(entityType)}
              indeterminate={_isUndetermined(entityType)}
              checked={isGroupSelected(entityType)}
              header={toDisplayTitle(entityType)}>
              {group
                .sort((a, b) => naturalSort(a.title, b.title))
                .map((entity) => {
                  const checked = isSelected(entity);
                  const header = _entityItemHeader(entity);

                  return (
                    <Input
                      key={entity.id}
                      type="checkbox"
                      formGroupClassName="form-group no-bm"
                      label={header}
                      checked={checked}
                      onChange={() => updateSelectionEntity(entity)}
                    />
                  );
                })}
            </ExpandableCheckboxListItem>
          );
        })}
    </ExpandableList>
  );
};

export default ContentPackSelectionList;
