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
import { DropdownButton, MenuItem } from 'components/bootstrap';
import { AddEvidence, AddEvidenceModal } from 'components/security/investigations';
import type { EvidenceTypes } from 'components/security/investigations/types';

type Props = {
  id: string,
  type: EvidenceTypes,
  index?: string,
  [key: string]: any,
};

const addToInvestigation = ({ investigationSelected }) => (
  <MenuItem disabled={!investigationSelected}>
    Add to investigation
  </MenuItem>
);

const InvestigationsMenu = ({ index, id, type, ...rest }: Props) => {
  const investigations = usePluginEntities('investigationsPlugin');

  if (!investigations || !investigations.length) return null;

  return (
    <DropdownButton id="investigations-dropdown" {...rest}>
      <AddEvidence index={index} id={id} type={type} child={addToInvestigation} />
      <AddEvidenceModal index={index}
                        id={id}
                        type={type}
                        child={<MenuItem>Select an investigation</MenuItem>} />
    </DropdownButton>
  );
};

InvestigationsMenu.defaultProps = {
  index: undefined,
};

export default InvestigationsMenu;
