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
import type * as Immutable from 'immutable';
import { useState, useEffect } from 'react';
import { Field, useFormikContext } from 'formik';
import styled from 'styled-components';

import { getValuesFromGRN } from 'logic/permissions/GRN';
import { Button, Alert, Input } from 'components/bootstrap';
import type SharedEntity from 'logic/permissions/SharedEntity';
import EntityShareDomain from 'domainActions/permissions/EntityShareDomain';
import Spinner from 'components/common/Spinner';
import Select from 'components/common/Select';
import useDashboards from 'views/components/dashboard/hooks/useDashboards';
import useStreams from 'components/streams/hooks/useStreams';
import useSavedSearches from 'views/hooks/useSavedSearches';
import type { SettingsFormValues } from 'components/users/UserEdit/SettingsSection';

const Container = styled.div`
  display: flex;
  align-items: center;
`;

const TypeSelect = styled(Select)`
  width: 200px;
  margin-right: 3px;
`;

const ValueSelect = styled(Select)`
  width: 400px;
`;

const ResetBtn = styled(Button)`
  margin-left: 5px;
`;

type Props = {
  userId: string;
  permissions: Immutable.List<string>;
}

type Option = {
  value: string;
  label: string;
}

// We cannot ask for all since the backend did not implement something like this. So for now its 10000.
const UNLIMITED_ENTITY_SHARE_REQ = { page: 1, perPage: 10000, query: '' };

const grnId = (grn) => getValuesFromGRN(grn).id;
const _grnOptionFormatter = ({ id, title }: SharedEntity): Option => ({ value: grnId(id), label: title });
const typeOptions = [
  { value: 'dashboard', label: 'Dashboard' },
  { value: 'stream', label: 'Stream' },
  { value: 'search', label: 'Search' },
];

const ADMIN_PERMISSION = '*';

const useStartPageOptions = (userId, permissions) => {
  const { values: { startpage } } = useFormikContext<SettingsFormValues>();
  const selectedUserIsAdmin = permissions.includes(ADMIN_PERMISSION);
  const [userDashboards, setUserDashboards] = useState<Option[]>([]);
  const [userStreams, setUserStreams] = useState<Option[]>([]);
  const [userSearches, setUserSearches] = useState<Option[]>([]);
  const [isLoadingUserEntities, setIsLoadingUserEntities] = useState(false);

  const { data: allDashboards, isInitialLoading: isLoadingAllDashboards } = useDashboards({ query: '', page: 1, pageSize: 0, sort: { direction: 'asc', attributeId: 'title' }, scope: 'read' }, { enabled: selectedUserIsAdmin });
  const { data: allStreams, isInitialLoading: isLoadingAllStreams } = useStreams({ query: '', page: 1, pageSize: 0, sort: { direction: 'asc', attributeId: 'title' } }, { enabled: selectedUserIsAdmin });
  const { data: allSearches, isInitialLoading: isLoadingAllSearches } = useSavedSearches({ query: '', page: 1, pageSize: 0, sort: { direction: 'asc', attributeId: 'title' } }, { enabled: selectedUserIsAdmin });
  const allDashboardsOptions = (allDashboards?.list ?? []).map(({ id, title }) => ({ value: id, label: title }));
  const allStreamsOptions = (allStreams?.list ?? []).map(({ id, title }) => ({ value: id, label: title }));
  const allSearchesOptions = (allSearches?.list ?? []).map(({ id, title }) => ({ value: id, label: title }));

  useEffect(() => {
    if (!selectedUserIsAdmin) {
      setIsLoadingUserEntities(true);

      EntityShareDomain.loadUserSharesPaginated(userId, {
        ...UNLIMITED_ENTITY_SHARE_REQ,
        additionalQueries: { entity_type: 'dashboard' },
      }).then(({ list }) => setUserDashboards(list.map(_grnOptionFormatter).toArray()))
        .then(() => EntityShareDomain.loadUserSharesPaginated(userId, {
          ...UNLIMITED_ENTITY_SHARE_REQ,
          additionalQueries: { entity_type: 'stream' },
        }).then(({ list }) => {
          setUserStreams(list.map(_grnOptionFormatter).toArray());
        })).then(() => EntityShareDomain.loadUserSharesPaginated(userId, {
          ...UNLIMITED_ENTITY_SHARE_REQ,
          additionalQueries: { entity_type: 'search' },
        }).then(({ list }) => {
          setIsLoadingUserEntities(false);
          setUserSearches(list.map(_grnOptionFormatter).toArray());
        }));
    }
  }, [selectedUserIsAdmin, userId]);

  const prepareOptions = () => {
    switch (startpage?.type) {
      case 'dashboard':
        return [...userDashboards, ...allDashboardsOptions];
      case 'search':
        return [{ value: 'default', label: 'New Search' }, ...userSearches, ...allSearchesOptions];
      case 'stream':
        return [...userStreams, ...allStreamsOptions];
      default:
        return [];
    }
  };

  return {
    options: prepareOptions(),
    isLoading: isLoadingUserEntities || isLoadingAllDashboards || isLoadingAllSearches || isLoadingAllStreams,
  };
};

const StartpageFormGroup = ({ userId, permissions }: Props) => {
  const { options, isLoading } = useStartPageOptions(userId, permissions);

  if (isLoading) {
    return <Spinner />;
  }

  return (
    <Field name="startpage">
      {({ field: { name, value, onChange }, meta }) => {
        const type = value?.type ?? 'dashboard';

        const error = value?.id && options.findIndex(({ value: v }) => v === value.id) < 0
          ? <Alert bsStyle="warning">User is missing permission for the configured page</Alert>
          : null;

        const resetBtn = value?.type
          ? (
            <ResetBtn onClick={() => onChange({ target: { name, value: {} } })}>
              Reset
            </ResetBtn>
          )
          : null;

        return (
          <Input id="startpage"
                 label="Start page"
                 help="Select the page the user sees right after log in. Only entities are selectable which the user has permissions for."
                 labelClassName="col-sm-3"
                 wrapperClassName="col-sm-9"
                 error={meta?.error}>
            <>
              <Container>
                <TypeSelect options={typeOptions}
                            placeholder="Select type"
                            onChange={(newType) => onChange({ target: { name, value: { type: newType, id: undefined } } })}
                            value={value?.type} />
                <ValueSelect options={options}
                             placeholder={`Select ${value?.type ?? 'entity'}`}
                             onChange={(newId) => onChange({ target: { name, value: { type: type, id: newId } } })}
                             value={value?.id} />
                {resetBtn}
              </Container>
              {error}
            </>
          </Input>
        );
      }}
    </Field>
  );
};

export default StartpageFormGroup;
