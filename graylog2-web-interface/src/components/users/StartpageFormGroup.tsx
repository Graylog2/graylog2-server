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
import * as Immutable from 'immutable';
import { useState, useEffect } from 'react';
import { Field } from 'formik';
import styled from 'styled-components';

import { getValuesFromGRN } from 'logic/permissions/GRN';
import { Button, Alert } from 'components/graylog';
import { Input } from 'components/bootstrap';
import SharedEntity from 'logic/permissions/SharedEntity';
import EntityShareDomain from 'domainActions/permissions/EntityShareDomain';
import Spinner from 'components/common/Spinner';
import Select from 'components/common/Select';
import { DashboardsActions } from 'views/stores/DashboardsStore';
import { StreamsActions } from 'stores/streams/StreamsStore';

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
];

const ADMIN_PERMISSION = '*';

const StartpageFormGroup = ({ userId, permissions }: Props) => {
  const [dashboards, setDashboards] = useState<Option[] | undefined>();
  const [streams, setStreams] = useState<Option[] | undefined>();

  useEffect(() => {
    if (permissions.includes(ADMIN_PERMISSION)) {
      DashboardsActions.search('', 1, 0).then(({ list }) => setDashboards(list.map(({ id, title }) => ({ value: id, label: title }))));

      StreamsActions.searchPaginated(1, 0, '').then(({ streams: streamsList }) => setStreams(streamsList.map(({ id, title }) => ({ value: id, label: title }))));
    } else {
      EntityShareDomain.loadUserSharesPaginated(userId, {
        ...UNLIMITED_ENTITY_SHARE_REQ,
        additionalQueries: { entity_type: 'dashboard' },
      }).then(({ list }) => setDashboards(list.map(_grnOptionFormatter).toArray()))
        .then(() => EntityShareDomain.loadUserSharesPaginated(userId, {
          ...UNLIMITED_ENTITY_SHARE_REQ,
          additionalQueries: { entity_type: 'stream' },
        }).then(({ list }) => setStreams(list.map(_grnOptionFormatter).toArray())));
    }
  }, [permissions, userId]);

  if (!streams || !dashboards) {
    return <Spinner />;
  }

  return (
    <Field name="startpage">
      {({ field: { name, value, onChange } }) => {
        const type = value?.type ?? 'dashboard';
        const options = type === 'dashboard' ? dashboards : streams;

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
          <>
            <Input id="startpage"
                   label="Start page"
                   help="Select the page the user sees right after log in. Only entities are selectable which the user has permissions for."
                   labelClassName="col-sm-3"
                   wrapperClassName="col-sm-9">
              <>
                <Container>
                  <TypeSelect options={typeOptions}
                              placeholder="Select type"
                              onChange={(newType) => onChange({ target: { name, value: { type: newType, id: undefined } } })}
                              value={value?.type} />
                  <ValueSelect options={options}
                               placeholder={`Select ${value?.type}`}
                               onChange={(newId) => onChange({ target: { name, value: { type: type, id: newId } } })}
                               value={value?.id} />
                  {resetBtn}
                </Container>
                {error}
              </>
            </Input>
          </>
        );
      }}
    </Field>
  );
};

export default StartpageFormGroup;
