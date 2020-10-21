// @flow strict
import React, { useState } from 'react';
import lodash from 'lodash';
import { PluginStore } from 'graylog-web-plugin/plugin';

import EntityShareModal from 'components/permissions/EntityShareModal';
import SharingDisabledPopover from 'components/permissions/SharingDisabledPopover';
import Routes from 'routing/Routes';
import { Link, LinkContainer } from 'components/graylog/router';
import {
  Button,
  DropdownButton,
  Label,
  MenuItem,
} from 'components/graylog';
import {
  EntityListItem,
  IfPermitted,
  HasOwnership,
} from 'components/common';

import EventDefinitionDescription from './EventDefinitionDescription';

type EventDefinition = {
  id: string,
  config: {
    type: string,
  },
  title: string,
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
  onDisable: (EventDefinition) => void,
  onEnable: (EventDefinition) => void,
  onDelete: (EventDefinition) => void,
};

const getConditionPlugin = (type) => {
  if (type === undefined) {
    return {};
  }

  return PluginStore.exports('eventDefinitionTypes').find((edt) => edt.type === type) || {};
};

const renderDescription = (definition, context) => {
  return <EventDefinitionDescription definition={definition} context={context} />;
};

const EventDefinitionEntry = ({
  context,
  eventDefinition,
  onDisable,
  onEnable,
  onDelete,
}: Props) => {
  const [showEntityShareModal, setShowEntityShareModal] = useState(false);
  const isScheduled = lodash.get(context, `scheduler.${eventDefinition.id}.is_scheduled`, true);

  let toggle = <MenuItem onClick={onDisable(eventDefinition)}>Disable</MenuItem>;

  if (!isScheduled) {
    toggle = <MenuItem onClick={onEnable(eventDefinition)}>Enable</MenuItem>;
  }

  const actions = (
    <React.Fragment key={`actions-${eventDefinition.id}`}>
      <IfPermitted permissions={`eventdefinitions:edit:${eventDefinition.id}`}>
        <LinkContainer to={Routes.ALERTS.DEFINITIONS.edit(eventDefinition.id)}>
          <Button bsStyle="info">Edit</Button>
        </LinkContainer>
      </IfPermitted>
      <IfPermitted permissions={`eventdefinitions:delete:${eventDefinition.id}`}>
        <DropdownButton id="more-dropdown" title="More" pullRight>
          {toggle}
          <MenuItem divider />
          <MenuItem onClick={onDelete(eventDefinition)}>Delete</MenuItem>
          <HasOwnership id={eventDefinition.id} type="event_definition">
            {({ disabled }) => (
              <MenuItem key={`share-${eventDefinition.id}`} onSelect={() => setShowEntityShareModal(true)} disabled={disabled}>
                Share {disabled && <SharingDisabledPopover type="stream" />}
              </MenuItem>
            )}
          </HasOwnership>
        </DropdownButton>
      </IfPermitted>
    </React.Fragment>
  );

  const plugin = getConditionPlugin(eventDefinition.config.type);
  let titleSuffix = plugin.displayName || eventDefinition.config.type;

  if (!isScheduled) {
    titleSuffix = (<span>{titleSuffix} <Label bsStyle="warning">disabled</Label></span>);
  }

  const linkTitle = <Link to={Routes.ALERTS.DEFINITIONS.view(eventDefinition.id)}>{eventDefinition.title}</Link>;

  return (
    <>
      <EntityListItem key={`event-definition-${eventDefinition.id}`}
                      title={linkTitle}
                      titleSuffix={titleSuffix}
                      description={renderDescription(eventDefinition, context)}
                      noItemsText="Could not find any items with the given filter."
                      actions={actions} />
      { showEntityShareModal && (
        <EntityShareModal entityId={eventDefinition.id}
                          entityType="event_definition"
                          entityTitle={eventDefinition.title}
                          description="Search for a User or Team to add as collaborator on this event definition."
                          onClose={() => setShowEntityShareModal(false)} />
      )}
    </>
  );
};

export default EventDefinitionEntry;
