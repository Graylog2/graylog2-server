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
import { useQuery } from '@tanstack/react-query';

import LookupTableParameterEdit from 'components/lookup-table-parameters/LookupTableParameterEdit';
import { fetchAllLookupTables } from 'components/lookup-tables/hooks/api/lookupTablesAPI';

type Props = {
  parameter: { lookupTable?: string; key?: string; defaultValue?: string; name?: string };
  onChange: (key: string, value: any) => void;
  identifier: string | number;
  validationState?: {
    lookupTable?: ['error' | 'warning' | 'success', string] | undefined;
    key?: ['error' | 'warning' | 'success', string] | undefined;
  };
};

const LookupTableQueryParameterEdit = ({ parameter, onChange, identifier, validationState = undefined }: Props) => {
  const { data: tables = [] } = useQuery({
    queryKey: ['lookup-tables', 'all'],
    queryFn: () => fetchAllLookupTables(),
  });

  return (
    <LookupTableParameterEdit
      lookupTables={tables}
      parameter={parameter}
      onChange={onChange}
      identifier={identifier}
      validationState={validationState}
    />
  );
};

export default LookupTableQueryParameterEdit;
