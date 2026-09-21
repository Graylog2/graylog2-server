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
import { useState } from 'react';
import styled, { css } from 'styled-components';

import { defaultCompare as naturalSort } from 'logic/DefaultCompare';
import type Entity from 'logic/content-packs/Entity';
import type EntityIndex from 'logic/content-packs/EntityIndex';
import { ExpandableList, Icon, ExpandableCheckboxListItem, Tooltip } from 'components/common';
import { Button, ButtonGroup, Input } from 'components/bootstrap';
import {
  CONTENT_PACK_SOURCE,
  SERVER_SOURCE,
  hasBothSources,
  rowEntityIds,
  sourceOf,
  type CatalogEntity,
  type EntityCatalog,
  type EntitySource,
} from 'logic/content-packs/EntityCatalog';

const HeaderText = styled.span`
  overflow-wrap: anywhere;
`;

const HeaderIcon = styled(Icon)(
  ({ theme }) => `
  padding-right: ${theme.spacings.xxs};
`,
);

const EntityRow = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
`;

const SourceActions = styled(ButtonGroup)(
  ({ theme }) => css`
    margin-left: ${theme.spacings.xs};
  `,
);

/*
 * A source that does not exist is only greyed out, not `disabled`, because a disabled button receives no mouse
 * events and could never show the tooltip that explains why it cannot be picked.
 */
const SourceButton = styled(Button)<{ $unavailable: boolean }>(
  ({ $unavailable }) => css`
    display: inline-flex;
    align-items: center;
    justify-content: center;

    ${$unavailable
      ? css`
          opacity: 0.4;
          cursor: not-allowed;
        `
      : ''}
  `,
);

const GroupActions = styled.div`
  display: flex;
  align-items: center;
  white-space: nowrap;
`;

/* The revision the pack copy came from is named in the help text above the list, not repeated on every control. */
const SOURCE_LABELS = {
  [SERVER_SOURCE]: 'All installed instances',
  [CONTENT_PACK_SOURCE]: 'All content pack instances',
};

const SOURCE_TITLES = {
  [SERVER_SOURCE]: 'Use the installed instance',
  [CONTENT_PACK_SOURCE]: 'Use the content pack instance',
};

const GROUP_SOURCE_TITLES = {
  [SERVER_SOURCE]: 'Use the installed instance for every entity checked in this group',
  [CONTENT_PACK_SOURCE]: 'Use the content pack instance for every entity checked in this group',
};

const MISSING_SOURCE_TITLES = {
  [SERVER_SOURCE]:
    'There is no installed instance of this entity on this server, either because it was never installed or ' +
    'because it has been deleted since. Only the content pack instance can be exported.',
  [CONTENT_PACK_SOURCE]:
    'This entity was not part of the content pack revision, so there is nothing to keep from it. ' +
    'Only the installed instance can be exported.',
};

const missingSources = (entity: CatalogEntity): Array<EntitySource> =>
  [
    ...(entity.serverEntity ? [] : [SERVER_SOURCE]),
    ...(entity.packEntity ? [] : [CONTENT_PACK_SOURCE]),
  ] as Array<EntitySource>;

/* Both icons inherit the colour of what they sit in, so the pair reads the same on a button and in the help text. */
const sourceIconName = (source: EntitySource) => (source === SERVER_SOURCE ? 'database' : 'package_2');

/* The gap belongs next to a label; on an icon-only button it would push the glyph off centre. */
const sourceIcon = (source: EntitySource, labelled = false) =>
  labelled ? <HeaderIcon name={sourceIconName(source)} /> : <Icon name={sourceIconName(source)} />;

const toDisplayTitle = (title: string) => {
  const newTitle = title.split('_').join(' ');

  return newTitle[0].toUpperCase() + newTitle.substr(1);
};

type SourceSelectProps = {
  /* Sources this entity does not have. They stay visible but disabled, so every row keeps the same controls. */
  missing?: Array<EntitySource>;
  onSelect: (source: EntitySource) => void;
  /* Left out on a group header, where the entities below can be on different sources. */
  selected?: EntitySource;
  /* Only the group header is labelled; the row control repeats per entity and stays icon-only. */
  showLabels?: boolean;
  size: 'xsmall' | 'small';
  titles: { [source: string]: string };
  missingTitles: { [source: string]: string };
  labels: { [source: string]: string };
};

const SourceSelect = ({
  labels,
  missing = [],
  missingTitles,
  onSelect,
  selected = undefined,
  showLabels = false,
  size,
  titles,
}: SourceSelectProps) => (
  <SourceActions>
    {[SERVER_SOURCE, CONTENT_PACK_SOURCE].map((source: EntitySource) => {
      const unavailable = missing.includes(source);
      const label = unavailable ? missingTitles[source] : titles[source];

      return (
        <Tooltip
          key={source}
          label={label}
          withArrow
          multiline={unavailable}
          w={unavailable ? 320 : undefined}
          position="top">
          <SourceButton
            $unavailable={unavailable}
            aria-disabled={unavailable}
            /* The buttons in a row carry no text, so the tooltip copy is their accessible name too. */
            aria-label={label}
            bsSize={size}
            bsStyle={selected === source && !unavailable ? 'info' : 'default'}
            active={selected === source && !unavailable}
            onClick={(e: React.MouseEvent) => {
              /* The row and the group header are both click targets themselves, so the click must not bubble. */
              e.preventDefault();
              e.stopPropagation();

              if (!unavailable) {
                onSelect(source);
              }
            }}>
            {sourceIcon(source, showLabels)}
            {showLabels && labels[source]}
          </SourceButton>
        </Tooltip>
      );
    })}
  </SourceActions>
);

type EntitySelectionProps = {
  entity: CatalogEntity;
  selected: EntityIndex | Entity | undefined;
  updateSelectionEntity: (entity: CatalogEntity) => void;
  updateEntitySource?: (entity: CatalogEntity, source: EntitySource) => void;
};

const EntitySelection = ({
  entity,
  selected,
  updateSelectionEntity,
  updateEntitySource = undefined,
}: EntitySelectionProps) => {
  const source = sourceOf(selected ?? entity.packEntity ?? entity.serverEntity);

  return (
    <EntityRow>
      <Input
        type="checkbox"
        formGroupClassName="form-group no-bm"
        label={<HeaderText>{entity.title}</HeaderText>}
        checked={!!selected}
        onChange={() => updateSelectionEntity(entity)}
      />
      {updateEntitySource && (
        <SourceSelect
          size="xsmall"
          selected={source}
          labels={SOURCE_LABELS}
          titles={SOURCE_TITLES}
          missingTitles={MISSING_SOURCE_TITLES}
          missing={missingSources(entity)}
          onSelect={(newSource) => updateEntitySource(entity, newSource)}
        />
      )}
    </EntityRow>
  );
};

type Props = {
  isFiltered: boolean;
  entities: EntityCatalog;
  selectedEntities: { [key: string]: Array<EntityIndex | Entity> };
  isGroupSelected: (group: string) => boolean;
  updateSelectionGroup: (group: string) => void;
  updateSelectionEntity: (entity: CatalogEntity) => void;
  updateGroupSource?: (group: string, source: EntitySource) => void;
  updateEntitySource?: (entity: CatalogEntity, source: EntitySource) => void;
};

const ContentPackSelectionList = ({
  isFiltered,
  entities,
  selectedEntities,
  isGroupSelected,
  updateSelectionGroup,
  updateSelectionEntity,
  updateGroupSource = undefined,
  updateEntitySource = undefined,
}: Props) => {
  const [expandedSections, setExpandedSections] = useState<Array<string>>([]);
  /* While a filter is active every group is expanded, so the matches are visible without expanding them by hand. */
  const visibleSections = isFiltered ? Object.keys(entities) : expandedSections;

  /* Looked up by id rather than scanned, so opening a group with hundreds of rows is not quadratic. */
  const selectedById = new Map(
    Object.values(selectedEntities).flatMap((selected) => selected.map((e) => [e.id, e] as const)),
  );

  const selectedEntity = (entity: CatalogEntity) =>
    rowEntityIds(entity)
      .map((id) => selectedById.get(id))
      .find(Boolean);

  const isUndetermined = (type: string) => {
    if (!selectedEntities[type]) {
      return false;
    }

    return !(selectedEntities[type].length === entities[type].length || selectedEntities[type].length === 0);
  };

  /* Only entities that offer both copies can be switched, so a group without any of them has nothing to act on. */
  const groupHasBothSources = (type: string) => entities[type].some(hasBothSources);

  /* `sort` is in place, so sorting the catalog arrays directly would reorder them on every render. */
  const sortedGroup = (type: string) => [...entities[type]].sort((a, b) => naturalSort(a.title, b.title));
  const entityTypes = [...Object.keys(entities)].sort((a, b) => naturalSort(a, b));

  return (
    <ExpandableList
        value={visibleSections}
        onChange={(newExpandedSections) => setExpandedSections(newExpandedSections)}>
        {entityTypes.map((entityType) => {
            const group = entities[entityType];

            if (group.length <= 0) {
              return null;
            }

            /* These are a shortcut for the row controls, not state, so they are left out when they would do nothing. */
            const showGroupActions =
              updateGroupSource && groupHasBothSources(entityType) && !!selectedEntities[entityType]?.length;

            const groupActions = showGroupActions ? (
              <GroupActions>
                <SourceSelect
                  size="xsmall"
                  showLabels
                  labels={SOURCE_LABELS}
                  titles={GROUP_SOURCE_TITLES}
                  missingTitles={MISSING_SOURCE_TITLES}
                  onSelect={(source) => updateGroupSource(entityType, source)}
                />
              </GroupActions>
            ) : undefined;

            return (
              <ExpandableCheckboxListItem
                key={entityType}
                value={entityType}
                onChange={() => updateSelectionGroup(entityType)}
                indeterminate={isUndetermined(entityType)}
                checked={isGroupSelected(entityType)}
                actions={groupActions}
                header={toDisplayTitle(entityType)}>
                {/* A collapsed group still mounts its children, so on a large pack only the open ones are built. */}
                {visibleSections.includes(entityType) &&
                  sortedGroup(entityType).map((entity) => (
                    <EntitySelection
                      key={entity.id}
                      entity={entity}
                      selected={selectedEntity(entity)}
                      updateSelectionEntity={updateSelectionEntity}
                      updateEntitySource={updateEntitySource}
                    />
                  ))}
              </ExpandableCheckboxListItem>
            );
          })}
    </ExpandableList>
  );
};

export default ContentPackSelectionList;
