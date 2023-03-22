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
import { useState, useEffect } from 'react';
import upperFirst from 'lodash/upperFirst';

import Routes from 'routing/Routes';
import { Link } from 'components/common/router';
import { IfPermitted, ReadOnlyFormGroup } from 'components/common';
import type User from 'logic/users/User';
import SectionComponent from 'components/common/Section/SectionComponent';
import { StreamsActions } from 'stores/streams/StreamsStore';
import { ViewManagementActions } from 'views/stores/ViewManagementStore';
import useIsGlobalTimeoutEnabled from 'hooks/useIsGlobalTimeoutEnabled';

type Props = {
  user: User,
};

const _sessionTimeout = (sessionTimeout: { value: number, unitString: string }, isGlobalTimeoutEnabled: boolean) => {
  if (sessionTimeout) {
    const globalTimeoutLink = <IfPermitted permissions={['clusterconfigentry:read']}>(<Link to={Routes.SYSTEM.CONFIGURATIONS}>globally set</Link>)</IfPermitted>;

    return <>{sessionTimeout.value} {sessionTimeout.unitString} {isGlobalTimeoutEnabled && globalTimeoutLink}</>;
  }

  return 'Sessions do not timeout';
};

const StartpageValue = ({ type, id }: { type: string | null | undefined, id: string | null | undefined }) => {
  const [title, setTitle] = useState<string | undefined>();

  useEffect(() => {
    if (!type || !id) {
      return;
    }

    if (type === 'stream') {
      StreamsActions.get(id).then(({ title: streamTitle }) => setTitle(streamTitle));
    } else {
      ViewManagementActions.get(id).then(({ title: viewTitle }) => setTitle(viewTitle));
    }
  }, [id, type]);

  if (!type || !id) {
    return <span>No start page set</span>;
  }

  const route = type === 'stream' ? Routes.stream_search(id) : Routes.dashboard_show(id);

  return (
    <Link to={route}><b>{upperFirst(type)}</b>:  {title}</Link>
  );
};

const SettingsSection = ({
  user: {
    timezone,
    serviceAccount,
    sessionTimeout,
    startpage,
  },
}: Props) => {
  const isGlobalTimeoutEnabled = useIsGlobalTimeoutEnabled();

  return (
    <SectionComponent title="Settings">
      <ReadOnlyFormGroup label="Sessions Timeout" value={_sessionTimeout(sessionTimeout, isGlobalTimeoutEnabled)} />
      <ReadOnlyFormGroup label="Service Account" value={serviceAccount} />
      <ReadOnlyFormGroup label="Timezone" value={timezone} />
      <ReadOnlyFormGroup label="Startpage" value={<StartpageValue type={startpage?.type} id={startpage?.id} />} />
    </SectionComponent>
  );
};

export default SettingsSection;
