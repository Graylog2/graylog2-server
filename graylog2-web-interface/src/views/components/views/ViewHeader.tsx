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

import React, { useCallback, useMemo, useState } from 'react';
import styled, { css } from 'styled-components';

import { useStore } from 'stores/connect';
import { ViewStore } from 'views/stores/ViewStore';
import { Icon } from 'components/common';
import { Row } from 'components/bootstrap';
import ViewPropertiesModal from 'views/components/views/DashboardPropertiesModal';
import onSaveView from 'views/logic/views/OnSaveViewAction';

const StyledList = styled.dl(() => css`
  display: flex;
  gap: 8px;
  margin: 0;
  flex-wrap: wrap;
  align-items: center;
  dd {
    font-style: italic;
  }
  dt {
    text-transform: capitalize;
  }
`);

const Content = styled.div(({ theme }) => css`
  display: flex;
  justify-content: space-between;
  flex-wrap: nowrap;
  align-items: center;
  padding-bottom: ${theme.spacings.sm}
`);
const EditButton = styled.div(() => css`
  width: 25px;
`);

const ViewHeader = () => {
  const { view } = useStore(ViewStore);
  const isSavedView = useMemo(() => view?.id && view?.title, [view]);
  const [showMetadataEdit, setShowMetadataEdit] = useState<boolean>(false);
  const toggleMetadataEdit = useCallback(() => setShowMetadataEdit((cur) => !cur), [setShowMetadataEdit]);

  return isSavedView ? (
    <Row>
      <Content>
        <StyledList>
          <dt>{view.type.toLocaleLowerCase()}:</dt>
          <dd>{view.title}</dd>
        </StyledList>
        <EditButton onClick={toggleMetadataEdit}
                    role="button"
                    title={`Edit ${view.type.toLocaleLowerCase()} ${view.title} metadata`}
                    tabIndex={0}>
          <Icon name="pen-to-square" />
        </EditButton>
        {showMetadataEdit && (
          <ViewPropertiesModal show
                               view={view}
                               title={`Editing saved ${view.type.toLocaleLowerCase()}`}
                               onClose={toggleMetadataEdit}
                               onSave={onSaveView} />
        )}
      </Content>
    </Row>
  ) : null;
};

export default ViewHeader;
