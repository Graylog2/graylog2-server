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
import { upperFirst } from 'lodash';

import Routes from 'routing/Routes';
import { Link } from 'components/graylog/router';
import { ReadOnlyFormGroup } from 'components/common';
import User from 'logic/users/User';
import SectionComponent from 'components/common/Section/SectionComponent';

type Props = {
  user: User,
};

const _sessionTimeout = (sessionTimeout) => {
  if (sessionTimeout) {
    return `${sessionTimeout.value} ${sessionTimeout.unitString}`;
  }

  return 'Sessions do not timeout';
};

const StartpageValue = ({ type, id }: { type: ?string, id: ?string }) => {
  if (!type || !id) {
    return <span>No Startpage set</span>;
  }

  const route = type === 'stream' ? Routes.stream_search(id) : Routes.dashboard_show(id);

  return (
    <Link to={route}>{upperFirst(type)} {id}</Link>
  );
};

const SettingsSection = ({
  user: {
    timezone,
    sessionTimeout,
    startpage,
  },
}: Props) => (
  <SectionComponent title="Settings">
    <ReadOnlyFormGroup label="Sessions Timeout" value={_sessionTimeout(sessionTimeout)} />
    <ReadOnlyFormGroup label="Timezone" value={timezone} />
    <ReadOnlyFormGroup label="Startpage" value={<StartpageValue type={startpage?.type} id={startpage?.id} />} />
  </SectionComponent>
);

export default SettingsSection;
