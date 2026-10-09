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
import styled, { css } from 'styled-components';

import { Spinner } from 'components/common';
import { Table } from 'components/bootstrap';
import useProfile from 'components/indices/IndexSetFieldTypeProfiles/hooks/useProfile';
import useFieldTypesForMappings from 'views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypesForMappings';
import StringUtils from 'util/StringUtils';

import ScrollContainer from './ScrollContainer';

const Container = styled.div(
  ({ theme }) => css`
    margin-bottom: ${theme.spacings.md};
  `,
);

type Props = {
  profileId: string;
};

const ProfileDetails = ({ profileId }: Props) => {
  const {
    data: { id, description, customFieldMappings, indexSetIds },
    isFetching,
  } = useProfile(profileId);
  const {
    data: { fieldTypes },
  } = useFieldTypesForMappings();

  if (isFetching || id !== profileId) {
    return <Spinner />;
  }

  const usageCount = indexSetIds?.length ?? 0;

  return (
    <Container>
      {description && <p>{description}</p>}
      <p>
        Already used by {usageCount} {StringUtils.pluralize(usageCount, 'index set', 'index sets')}.
      </p>
      <ScrollContainer $maxHeight={200}>
        <Table condensed striped>
          <thead>
            <tr>
              <th>Field</th>
              <th>Type</th>
            </tr>
          </thead>
          <tbody>
            {customFieldMappings.map(({ field, type }) => (
              <tr key={field}>
                <td>{field}</td>
                <td>{fieldTypes[type] ?? type}</td>
              </tr>
            ))}
          </tbody>
        </Table>
      </ScrollContainer>
    </Container>
  );
};

export default ProfileDetails;
