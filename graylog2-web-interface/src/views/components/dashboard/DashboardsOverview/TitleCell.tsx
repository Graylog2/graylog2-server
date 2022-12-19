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
import React from 'react';

import type View from 'views/logic/views/View';
import type { Requirements } from 'views/logic/views/View';
import { HoverForHelp } from 'components/common';
import { Link } from 'components/common/router';
import Routes from 'routing/Routes';

const missingRequirements = (requires: Requirements, requirementsProvided: Array<string>) => (
  Object.entries(requires)
    .filter(([require]) => !requirementsProvided.includes(require))
    .reduce((prev, [key, value]) => ({ ...prev, [key]: value }), {})
);

const RequirementsList = ({ requirements }: { requirements: Requirements }) => (
  <div>
    {Object.values(requirements).map(({ url, name }) => (
      <a href={url} target="_blank" rel="noopener noreferrer"><strong>{name}</strong></a>
    ))}
  </div>
);

type Props = {
  dashboard: View,
  requirementsProvided: Array<string>,
}

const TitleCell = ({ dashboard: { id, requires, title }, requirementsProvided }: Props) => {
  const _missingRequirements = missingRequirements(requires, requirementsProvided);
  const isMissingRequirements = Object.keys(_missingRequirements).length > 0;

  if (isMissingRequirements) {
    return (
      <>
        {title}
        <HoverForHelp title="Missing Requirements"><RequirementsList requirements={_missingRequirements} /></HoverForHelp>
      </>
    );
  }

  return <Link to={Routes.pluginRoute('DASHBOARDS_VIEWID')(id)}>{title}</Link>;
};

export default TitleCell;
