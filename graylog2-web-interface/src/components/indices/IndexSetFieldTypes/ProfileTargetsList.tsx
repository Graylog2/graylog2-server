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

import { Badge, Table } from 'components/bootstrap';
import StringUtils from 'util/StringUtils';

import ScrollContainer from './ScrollContainer';
import type { ProfileTargetIndexSet, SkippedIndexSet } from './profileTargets';

const Container = styled.div(
  ({ theme }) => css`
    margin-bottom: ${theme.spacings.md};
  `,
);

const Summary = styled.p(
  ({ theme }) => css`
    margin-bottom: ${theme.spacings.xs};
  `,
);

const StatusHeader = styled.th`
  width: 120px;
`;

const Note = styled.span(
  ({ theme }) => css`
    color: ${theme.colors.text.secondary};
  `,
);

const pluralizeIndexSets = (count: number) => `${count} ${StringUtils.pluralize(count, 'index set', 'index sets')}`;

type Props = {
  affected: Array<ProfileTargetIndexSet>;
  skipped: Array<SkippedIndexSet>;
  rotated: boolean;
};

const ProfileTargetsList = ({ affected, skipped, rotated }: Props) => (
  <Container>
    <h5>
      <b>Index Sets</b>
    </h5>
    <Summary>
      Applies to {pluralizeIndexSets(affected.length)}
      {skipped.length > 0 && `, excludes ${skipped.length}`}.
    </Summary>
    <ScrollContainer $maxHeight={250}>
      <Table condensed>
        <thead>
          <tr>
            <th>Index set</th>
            <StatusHeader>Status</StatusHeader>
            <th>Note</th>
          </tr>
        </thead>
        <tbody>
          {affected.map(({ id, title, writable }) => (
            <tr key={id}>
              <td>{title}</td>
              <td>
                <Badge color="success" variant="light">
                  Included
                </Badge>
              </td>
              <td>{!writable && <Note>{rotated ? 'Read-only, not rotated' : 'Read-only'}</Note>}</td>
            </tr>
          ))}
          {skipped.map(({ indexSet, reason }) => (
            <tr key={indexSet.id}>
              <td>{indexSet.title}</td>
              <td>
                <Badge color="gray" variant="light">
                  Excluded
                </Badge>
              </td>
              <td>
                <Note>{reason}</Note>
              </td>
            </tr>
          ))}
        </tbody>
      </Table>
    </ScrollContainer>
  </Container>
);

export default ProfileTargetsList;
