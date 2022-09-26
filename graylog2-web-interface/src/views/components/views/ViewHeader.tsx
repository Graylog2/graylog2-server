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

import { Link } from 'components/common/router';
import { useStore } from 'stores/connect';
import { ViewStore } from 'views/stores/ViewStore';
import { Icon } from 'components/common';
import { Row } from 'components/bootstrap';
import ViewPropertiesModal from 'views/components/views/DashboardPropertiesModal';
import onSaveView from 'views/logic/views/OnSaveViewAction';
import View from 'views/logic/views/View';
import Routes from 'routing/Routes';

const links = {
  [View.Type.Dashboard]: {
    link: Routes.DASHBOARDS,
    label: 'Dashboards',
  },
  [View.Type.Search]: {
    link: Routes.SEARCH,
    label: 'Search',
  },
};

const Content = styled.div(({ theme }) => css`
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
  padding-bottom: ${theme.spacings.sm};
  gap: 4px;
  margin: 0;
`);
const EditButton = styled.div(({ theme }) => css`
  color: ${theme.colors.gray[60]};
  font-size: ${theme.fonts.size.tiny}
`);
const TitleWrapper = styled.span`
  display: flex;
  gap: 4px;
  align-items: center;
  cursor: pointer;
  & ${EditButton} {
    display: none;
  }
  &:hover ${EditButton} {
    display: block;
  }
`;

const StyledIcon = styled(Icon)`
font-size: 0.50rem;
`;

const ViewHeader = () => {
  const { view } = useStore(ViewStore);
  const isSavedView = view?.id && view?.title;
  const [showMetadataEdit, setShowMetadataEdit] = useState<boolean>(false);
  const toggleMetadataEdit = useCallback(() => setShowMetadataEdit((cur) => !cur), [setShowMetadataEdit]);

  const typeText = useMemo<string>(() => view.type.toLocaleLowerCase(), [view]);

  return (
    <Row>
      <Content>
        <Link to={links[view.type].link}>
          {links[view.type].label}
        </Link>
        <StyledIcon name="chevron-right" />
        <TitleWrapper>
          <span>{view.title || <i>{`Unsaved ${typeText}`}</i>}</span>
          {isSavedView && (
          <EditButton onClick={toggleMetadataEdit}
                      role="button"
                      title={`Edit ${typeText} ${view.title} metadata`}
                      tabIndex={0}>
            <Icon name="pen-to-square" />
          </EditButton>
          )}
        </TitleWrapper>
        {showMetadataEdit && (
        <ViewPropertiesModal show
                             view={view}
                             title={`Editing saved ${typeText}`}
                             onClose={toggleMetadataEdit}
                             onSave={onSaveView} />
        )}
      </Content>
    </Row>
  );
};

export default ViewHeader;
