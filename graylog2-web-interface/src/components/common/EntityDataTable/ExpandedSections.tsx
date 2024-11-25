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
import { useContext } from 'react';
import styled, { css } from 'styled-components';

import IconButton from 'components/common/IconButton';
import { ButtonToolbar } from 'components/bootstrap';

import type { EntityBase, ExpandedSectionRenderer } from './types';
import ExpandedEntitiesSectionsContext from './contexts/ExpandedSectionsContext';

const Container = styled.tr(({ theme }) => css`
  &&&& {
    background-color: ${theme.colors.global.contentBackground};
  }
`);

const Header = styled.div`
  display: flex;
  justify-content: space-between;
  margin-bottom: 5px;
`;

const Actions = styled(ButtonToolbar)`
  display: flex;
  align-items: center;
`;

const HideSectionButton = styled(IconButton)`
  margin-left: 5px;
`;

const ExpandedSections = <Entity extends EntityBase>({
  expandedSectionsRenderer,
  entity,
}: {
  expandedSectionsRenderer: {
    [sectionName: string]: ExpandedSectionRenderer<Entity>
  } | undefined,
  entity: Entity
}) => {
  const { expandedSections, toggleSection } = useContext(ExpandedEntitiesSectionsContext);
  const expandedEntitySections = expandedSections?.[entity.id];

  if (!expandedEntitySections?.length) {
    return null;
  }

  return (
    <Container>
      <td colSpan={1000}>
        {Object.entries(expandedSectionsRenderer ?? {})
          .filter(([sectionName]) => expandedEntitySections.includes(sectionName))
          .map(([sectionName, section]) => {
            const hideSection = () => toggleSection(entity.id, sectionName);
            const actions = section.actions?.(entity);

            return (
              <div key={`${sectionName}-${entity.id}`}>
                {section.disableHeader !== true
                  ? (
                    <Header>
                      <h3>{section.title}</h3>
                      <Actions>
                        {actions}
                        <HideSectionButton name="close" onClick={hideSection} title="Hide section" />
                      </Actions>
                    </Header>
                  ) : null}
                {section.content(entity)}
              </div>
            );
          })}
      </td>
    </Container>
  );
};

export default ExpandedSections;
