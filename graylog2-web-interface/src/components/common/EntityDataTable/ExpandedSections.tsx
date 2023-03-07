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
import styled from 'styled-components';

import type { EntityBase, ExpandedSectionRenderer } from 'components/common/EntityDataTable/types';
import ExpandedEntitiesSectionsContext from 'components/common/EntityDataTable/ExpandedSectionsContext';
import { IconButton } from 'components/common';
import { ButtonToolbar } from 'components/bootstrap';

const Header = styled.div`
  display: flex;
  justify-content: space-between;
  margin-bottom: 5px;
`;

const Actions = styled(ButtonToolbar)`
  display: flex;
  align-items: center;
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
    <tr>
      <td colSpan={1000}>
        {Object.entries(expandedSectionsRenderer ?? {}).map(([sectionName, section]) => {
          if (!expandedEntitySections.includes(sectionName)) {
            return null;
          }

          const hideSection = () => toggleSection(entity.id, sectionName);

          return (
            <div>
              <Header>
                <h3>{section.title}</h3>
                <Actions>
                  {section.actions?.(entity)}
                  <IconButton name="times" onClick={hideSection} />
                </Actions>
              </Header>
              {section.content(entity)}
            </div>
          );
        })}
      </td>
    </tr>
  );
};

export default ExpandedSections;
