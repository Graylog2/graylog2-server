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
import { useState, useCallback } from 'react';

import { ShareButton, IfPermitted, HoverForHelp } from 'components/common';
import { Button, ButtonToolbar, MenuItem } from 'components/bootstrap';
import type { Stream, StreamRule } from 'stores/streams/StreamsStore';
import StreamsStore from 'stores/streams/StreamsStore';
import Routes from 'routing/Routes';
import { StartpageStore } from 'stores/users/StartpageStore';
import StreamRuleModal from 'components/streamrules/StreamRuleModal';
import EntityShareModal from 'components/permissions/EntityShareModal';
import { StreamRulesStore } from 'stores/streams/StreamRulesStore';
import useCurrentUser from 'hooks/useCurrentUser';
import type { IndexSet } from 'stores/indices/IndexSetsStore';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import useSelectedEntities from 'components/common/EntityDataTable/hooks/useSelectedEntities';
import { MoreActions } from 'components/common/EntityDataTable';
import { LinkContainer } from 'components/common/router';
import HideOnCloud from 'util/conditional/HideOnCloud';
import UserNotification from 'util/UserNotification';
import StreamDeleteModal from 'components/streams/StreamsOverview/StreamDeleteModal';
import StreamModal from 'components/streams/StreamModal';

const DefaultStreamHelp = () => (
  <HoverForHelp displayLeftMargin>Action not available for the default
    stream
  </HoverForHelp>
);

const EventActions = ({ listItem }) => (
  <ButtonToolbar>
    <Button bsStyle="primary"
            bsSize="xsmall"
            onClick={() => {}}>Data Routing
    </Button>
  </ButtonToolbar>
);

export default EventActions;
