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
import React, { useState } from 'react';
import lodash from 'lodash';
import { PluginStore } from 'graylog-web-plugin/plugin';

import useGetPermissionsByScope from 'hooks/useScopePermissions';
import EntityShareModal from 'components/permissions/EntityShareModal';
import Routes from 'routing/Routes';
import { Link, LinkContainer } from 'components/common/router';
import {
  EntityListItem,
  IfPermitted,
  Icon,
  ShareButton,
  Spinner,
} from 'components/common';
import {
  Button,
  DropdownButton,
  Label,
  MenuItem,
} from 'components/bootstrap';
import ButtonToolbar from 'components/bootstrap/ButtonToolbar';

import EventDefinitionDescription from './EventDefinitionDescription';

export type EventDefinition = {
  id: string,
  config: {
    type: string,
  },
  title: string,
  _scope: string,
};

type Props = {
  context: {
    scheduler: {
      [id: string]: {
        is_scheduled: boolean,
      },
    },
  },
  eventDefinition: EventDefinition,
  onDisable: (eventDefinition: EventDefinition) => void,
  onEnable: (eventDefinition: EventDefinition) => void,
  onDelete: (eventDefinition: EventDefinition) => void,
  onCopy: (eventDefinition: EventDefinition) => void,
};

const getConditionPlugin = (type: string) => PluginStore.exports('eventDefinitionTypes')
  .find((edt) => edt.type === type);

const renderDescription = (definition, context) => {
  return <EventDefinitionDescription definition={definition} context={context} />;
};

const EventDefinitionEntry = ({
  context,
  eventDefinition,
  onDisable,
  onEnable,
  onDelete,
  onCopy,
}: Props) => {
  const [showEntityShareModal, setShowEntityShareModal] = useState(false);
  const isScheduled = lodash.get(context, `scheduler.${eventDefinition.id}.is_scheduled`, true);
  const { loadingScopePermissions, scopePermissions } = useGetPermissionsByScope(eventDefinition);

  const showActions = (): boolean => {
    return scopePermissions?.is_mutable;
  };

  const handleCopy = () => {
    onCopy(eventDefinition);
  };

  const handleDelete = () => {
    onDelete(eventDefinition);
  };

  const handleDisable = () => {
    onDisable(eventDefinition);
  };

  const handleEnable = () => {
    onEnable(eventDefinition);
  };

  let toggle = <MenuItem onClick={handleDisable}>Disable</MenuItem>;

  if (!isScheduled) {
    toggle = <MenuItem onClick={handleEnable}>Enable</MenuItem>;
  }

  const actions = (
    <ButtonToolbar key={`actions-${eventDefinition.id}`}>
      {showActions() && (
        <IfPermitted permissions={`eventdefinitions:edit:${eventDefinition.id}`}>
          <LinkContainer to={Routes.ALERTS.DEFINITIONS.edit(eventDefinition.id)}>
            <Button data-testid="edit-button">
              <Icon name="edit" /> Edit
            </Button>
          </LinkContainer>
        </IfPermitted>
      )}
      <ShareButton entityId={eventDefinition.id} entityType="event_definition" onClick={() => setShowEntityShareModal(true)} />
      <IfPermitted permissions={`eventdefinitions:delete:${eventDefinition.id}`}>
        <DropdownButton id="more-dropdown" title="More" pullRight>
          <MenuItem onClick={handleCopy}>Duplicate</MenuItem>
          <MenuItem divider />
          {toggle}
          {showActions() && (
            <>
              <MenuItem divider />
              <MenuItem onClick={handleDelete} data-testid="delete-button">Delete</MenuItem>
            </>
          )}
        </DropdownButton>
      </IfPermitted>
    </ButtonToolbar>
  );

  const plugin = getConditionPlugin(eventDefinition.config.type);
  let titleSuffix = <div>{plugin?.displayName ?? eventDefinition.config.type}</div>;

  if (!isScheduled) {
    titleSuffix = (<span>{titleSuffix} <Label bsStyle="warning">disabled</Label></span>);
  }

  const linkTitle = <Link to={Routes.ALERTS.DEFINITIONS.show(eventDefinition.id)}>{eventDefinition.title}</Link>;

  if (loadingScopePermissions) {
    return (
      <Spinner text="Loading Event Definitions" />
    );
  }

  return (
    <>
      <EntityListItem key={`event-definition-${eventDefinition.id}`}
                      title={linkTitle}
                      titleSuffix={titleSuffix}
                      description={renderDescription(eventDefinition, context)}
                      actions={actions} />
      {showEntityShareModal && (
        <EntityShareModal entityId={eventDefinition.id}
                          entityType="event_definition"
                          entityTypeTitle="event definition"
                          entityTitle={eventDefinition.title}
                          description="Search for a User or Team to add as collaborator on this event definition."
                          onClose={() => setShowEntityShareModal(false)} />
      )}
    </>
  );
};

export default EventDefinitionEntry;
