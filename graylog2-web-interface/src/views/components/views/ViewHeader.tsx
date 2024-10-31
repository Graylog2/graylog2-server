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
import { Icon } from 'components/common';
import { Row } from 'components/bootstrap';
import ViewPropertiesModal from 'views/components/dashboard/DashboardPropertiesModal';
import onSaveView from 'views/logic/views/OnSaveViewAction';
import View from 'views/logic/views/View';
import Routes from 'routing/Routes';
import useViewTitle from 'views/hooks/useViewTitle';
import useView from 'views/hooks/useView';
import useAppDispatch from 'stores/useAppDispatch';
import FavoriteIcon from 'views/components/FavoriteIcon';
import useAlertAndEventDefinitionData from 'hooks/useAlertAndEventDefinitionData';
import { updateView } from 'views/logic/slices/viewSlice';
import useIsNew from 'views/hooks/useIsNew';
import { createGRN } from 'logic/permissions/GRN';
import ExecutionInfo from 'views/components/views/ExecutionInfo';

const links = {
  [View.Type.Dashboard]: ({ id, title }) => [{
    link: Routes.DASHBOARDS,
    label: 'Dashboards',
  },
  {
    label: title || id,
    dataTestId: 'view-title',
  },
  ],
  [View.Type.Search]: ({ id, title }) => [{
    link: Routes.SEARCH,
    label: 'Search',
  },
  {
    label: title || id,
    dataTestId: 'view-title',
  },
  ],
  alert: ({ id }) => [
    {
      link: Routes.ALERTS.LIST,
      label: 'Alerts & Events',
    },
    {
      label: id,
      dataTestId: 'alert-id-title',
    },
  ],
  eventDefinition: ({ id, title }) => [
    {
      link: Routes.ALERTS.DEFINITIONS.LIST,
      label: 'Event definitions',
    },
    {
      link: Routes.ALERTS.DEFINITIONS.show(id),
      label: title || id,
      dataTestId: 'event-definition-title',
    },
  ],
};

const Content = styled.div(({ theme }) => css`
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
  margin-bottom: ${theme.spacings.xs};
  gap: 4px;
`);

const ExecutionInfoContainer = styled.div`
  margin-left: auto;
`;

const EditButton = styled.div(({ theme }) => css`
  color: ${theme.colors.gray[60]};
  font-size: ${theme.fonts.size.tiny};
  cursor: pointer;
`);

const TitleWrapper = styled.span`
  display: flex;
  gap: 4px;
  align-items: center;

  & ${EditButton} {
    display: none;
  }

  &:hover ${EditButton} {
    display: block;
  }
`;

const StyledIcon = styled(Icon)`
font-size: 0.5rem;
`;

const CrumbLink = ({ label, link, dataTestId }: { label: string, link: string | undefined, dataTestId?: string}) => (
  link ? <Link target="_blank" to={link} data-testid={dataTestId}>{label}</Link> : <span data-testid={dataTestId}>{label}</span>
);

const ViewHeader = () => {
  const view = useView();
  const isNew = useIsNew();
  const isSavedView = view?.id && view?.title && !isNew;
  const [showMetadataEdit, setShowMetadataEdit] = useState<boolean>(false);
  const toggleMetadataEdit = useCallback(() => setShowMetadataEdit((cur) => !cur), [setShowMetadataEdit]);

  const { alertId, definitionId, definitionTitle, isAlert, isEventDefinition, isEvent } = useAlertAndEventDefinitionData();
  const dispatch = useAppDispatch();
  const _onSaveView = useCallback(async (updatedView: View) => {
    await dispatch(onSaveView(updatedView));
    await dispatch(updateView(updatedView));
  }, [dispatch]);

  const typeText = view?.type?.toLocaleLowerCase();
  const title = useViewTitle();
  const onChangeFavorite = useCallback((newValue: boolean) => dispatch(updateView(view.toBuilder().favorite(newValue).build())), [dispatch, view]);

  const breadCrumbs = useMemo(() => {
    if (isAlert || isEvent) return links.alert({ id: alertId });
    if (isEventDefinition) return links.eventDefinition({ id: definitionId, title: definitionTitle });

    return links[view.type]({ id: view.id, title });
  }, [alertId, definitionId, definitionTitle, isAlert, isEvent, isEventDefinition, view, title]);

  const showExecutionInfo = view.type === 'SEARCH';

  return (
    <Row>
      <Content>
        {
          breadCrumbs.map(({ label, link, dataTestId }, index) => {
            const theLast = index === breadCrumbs.length - 1;

            return (
              <TitleWrapper key={`${label}_${link}`}>
                <CrumbLink link={link} label={label} dataTestId={dataTestId} />
                {!theLast && <StyledIcon name="chevron_right" />}
                {isSavedView && theLast && (
                  <>
                    <FavoriteIcon isFavorite={view.favorite} grn={createGRN(view.type, view.id)} onChange={onChangeFavorite} />
                    <EditButton onClick={toggleMetadataEdit}
                                role="button"
                                title={`Edit ${typeText} ${view.title} metadata`}
                                tabIndex={0}>
                      <Icon name="edit_square" />
                    </EditButton>
                  </>
                )}
              </TitleWrapper>
            );
          })
        }
        {showMetadataEdit && (
        <ViewPropertiesModal show
                             view={view}
                             title={`Editing saved ${typeText}`}
                             onClose={toggleMetadataEdit}
                             onSave={_onSaveView}
                             submitButtonText={`Save ${typeText}`} />
        )}
        {showExecutionInfo && <ExecutionInfoContainer><ExecutionInfo /></ExecutionInfoContainer>}
      </Content>
    </Row>
  );
};

export default ViewHeader;
