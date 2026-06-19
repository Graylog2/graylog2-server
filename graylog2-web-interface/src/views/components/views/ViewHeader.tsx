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

import React, { useCallback, useContext, useMemo, useState } from 'react';
import styled, { css } from 'styled-components';

import { Link, Icon } from 'components/common';
import { Row } from 'components/bootstrap';
import ViewPropertiesModal from 'views/components/dashboard/DashboardPropertiesModal';
import onSaveView from 'views/logic/views/OnSaveViewAction';
import View from 'views/logic/views/View';
import Routes from 'routing/Routes';
import useViewTitle from 'views/hooks/useViewTitle';
import useView from 'views/hooks/useView';
import useViewsDispatch from 'views/stores/useViewsDispatch';
import FavoriteIcon from 'views/components/FavoriteIcon';
import { updateView } from 'views/logic/slices/viewSlice';
import useIsNew from 'views/hooks/useIsNew';
import { createGRN } from 'logic/permissions/GRN';
import useAlertAndEventDefinitionData from 'components/event-definitions/replay-search/hooks/useAlertAndEventDefinitionData';
import useReplaySearchContext from 'components/event-definitions/replay-search/hooks/useReplaySearchContext';
import ViewsExecutionInfo from 'views/components/views/ViewsExecutionInto';
import RightSidebarContext from 'contexts/RightSidebarContext';
import SidebarEventDetails from 'components/events/SidebarEventDetails';
import SidebarEventDefinitionDetails from 'components/event-definitions/SidebarEventDefinitionDetails';
import Button from 'components/bootstrap/Button';

type Crumb = {
  label: string;
  link?: string;
  onClick?: () => void;
  dataTestId?: string;
};

type LinkParams = {
  id: string;
  title?: string;
  onShowDetails?: () => void;
};

const links: Record<string, (params: LinkParams) => Array<Crumb>> = {
  [View.Type.Dashboard]: ({ id, title }) => [
    {
      link: Routes.DASHBOARDS,
      label: 'Dashboards',
    },
    {
      label: title || id,
      dataTestId: 'view-title',
    },
  ],
  [View.Type.Search]: ({ id, title }) => [
    {
      link: Routes.SEARCH,
      label: 'Search',
    },
    {
      label: title || id,
      dataTestId: 'view-title',
    },
  ],
  alert: ({ id, onShowDetails }) => [
    {
      link: Routes.ALERTS.LIST,
      label: 'Alerts & Events',
    },
    {
      label: id,
      onClick: onShowDetails,
      dataTestId: 'alert-id-title',
    },
  ],
  eventDefinition: ({ id, title, onShowDetails }) => [
    {
      link: Routes.ALERTS.DEFINITIONS.LIST,
      label: 'Event definitions',
    },
    {
      label: title || id,
      onClick: onShowDetails,
      dataTestId: 'event-definition-title',
    },
  ],
};

const Content = styled.div(
  ({ theme }) => css`
    display: flex;
    flex-wrap: nowrap;
    align-items: center;
    margin-bottom: ${theme.spacings.xs};
    gap: ${theme.spacings.sm};
    justify-content: space-between;
  `,
);

const Breadcrumb = styled.div(
  ({ theme }) => css`
    display: flex;
    flex-wrap: nowrap;
    align-items: center;
    gap: ${theme.spacings.xxs};
  `,
);

const EditButton = styled.div(
  ({ theme }) => css`
    color: ${theme.colors.gray[60]};
    font-size: ${theme.fonts.size.tiny};
    cursor: pointer;
  `,
);

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

const CrumbLink = ({
  label,
  link,
  onClick = undefined,
  dataTestId = undefined,
}: {
  label: string;
  link: string | undefined;
  onClick?: () => void;
  dataTestId?: string;
}) => {
  if (link) {
    return (
      <Link target="_blank" to={link} data-testid={dataTestId}>
        {label}
      </Link>
    );
  }

  if (onClick) {
    return (
      <Button bsStyle="link" onClick={onClick} data-testid={dataTestId}>
        {label}
      </Button>
    );
  }

  return <span data-testid={dataTestId}>{label}</span>;
};

const ViewHeader = () => {
  const view = useView();
  const isNew = useIsNew();
  const isSavedView = view?.id && view?.title && !isNew;
  const [showMetadataEdit, setShowMetadataEdit] = useState<boolean>(false);
  const toggleMetadataEdit = useCallback(() => setShowMetadataEdit((cur) => !cur), [setShowMetadataEdit]);

  const { alertId, definitionId, type } = useReplaySearchContext();
  const { definitionTitle } = useAlertAndEventDefinitionData(alertId, definitionId);
  const dispatch = useViewsDispatch();
  const rightSidebar = useContext(RightSidebarContext);
  const showAlertDetails = useCallback(
    () => rightSidebar?.openSidebar(SidebarEventDetails(alertId, definitionId)),
    [rightSidebar, alertId, definitionId],
  );
  const showEventDefinitionDetails = useCallback(
    () => rightSidebar?.openSidebar(SidebarEventDefinitionDetails(definitionId)),
    [rightSidebar, definitionId],
  );
  const _onSaveView = useCallback(
    async (updatedView: View) => {
      await dispatch(onSaveView(updatedView));
      await dispatch(updateView(updatedView));
    },
    [dispatch],
  );

  const typeText = view?.type?.toLocaleLowerCase();
  const title = useViewTitle();
  const onChangeFavorite = useCallback(
    (newValue: boolean) => dispatch(updateView(view.toBuilder().favorite(newValue).build())),
    [dispatch, view],
  );

  const canShowDetails = !!rightSidebar;
  const breadcrumbs = useMemo(() => {
    switch (type) {
      case 'alert':
      case 'event':
        return links.alert({ id: alertId, onShowDetails: canShowDetails ? showAlertDetails : undefined });
      case 'event_definition':
        return links.eventDefinition({
          id: definitionId,
          title: definitionTitle,
          onShowDetails: canShowDetails ? showEventDefinitionDetails : undefined,
        });
      default:
        return links[view.type]({ id: view.id, title });
    }
  }, [
    type,
    alertId,
    canShowDetails,
    showAlertDetails,
    definitionId,
    definitionTitle,
    showEventDefinitionDetails,
    view.type,
    view.id,
    title,
  ]);

  const showExecutionInfo = view.type === 'SEARCH';

  return (
    <Row>
      <Content>
        <Breadcrumb>
          {breadcrumbs.map(({ label, link, onClick, dataTestId }, index) => {
            const theLast = index === breadcrumbs.length - 1;

            return (
              <TitleWrapper key={`${label}_${link}`}>
                <CrumbLink link={link} label={label} onClick={onClick} dataTestId={dataTestId} />
                {!theLast && <StyledIcon name="chevron_right" />}
                {isSavedView && theLast && (
                  <>
                    <FavoriteIcon
                      isFavorite={view.favorite}
                      grn={createGRN(view.type, view.id)}
                      onChange={onChangeFavorite}
                    />
                    <EditButton
                      onClick={toggleMetadataEdit}
                      role="button"
                      title={`Edit ${typeText} ${view.title} metadata`}
                      tabIndex={0}>
                      <Icon name="edit_square" />
                    </EditButton>
                  </>
                )}
              </TitleWrapper>
            );
          })}
          {showMetadataEdit && (
            <ViewPropertiesModal
              show
              view={view}
              title={`Editing saved ${typeText}`}
              onClose={toggleMetadataEdit}
              onSave={_onSaveView}
              submitButtonText={`Save ${typeText}`}
            />
          )}
        </Breadcrumb>
        {showExecutionInfo && <ViewsExecutionInfo />}
      </Content>
    </Row>
  );
};

export default ViewHeader;
