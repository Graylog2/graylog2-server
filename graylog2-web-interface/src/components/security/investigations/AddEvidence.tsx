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

import usePluginEntities from 'hooks/usePluginEntities';

import type { AddEvidenceProps } from './types';

type Props = AddEvidenceProps & {
  investigationSelected?: (arg: boolean) => void,
};

function AddEvidence({ children, index, id, type, investigationSelected }: Props) {
  const investigations = usePluginEntities('investigationsPlugin');

  if (!investigations || !investigations.length) return null;

  const { selectedInvestigationId } = investigations[0].hooks.useInvestigationDrawer();

  if (!selectedInvestigationId) {
    if (investigationSelected) investigationSelected(!!selectedInvestigationId);

    return children as React.ReactElement;
  }

  const SecAddEvidence = investigations[0].components.AddEvidence;

  return (
    <SecAddEvidence id={id} index={index} type={type}>
      {children}
    </SecAddEvidence>
  );
}

AddEvidence.defaultProps = {
  investigationSelected: undefined,
};

export default AddEvidence;
