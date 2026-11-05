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
import { useTranslation } from 'react-i18next';
import styled, { css } from 'styled-components';

import { Badge, Table } from 'components/bootstrap';

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

type Props = {
  affected: Array<ProfileTargetIndexSet>;
  skipped: Array<SkippedIndexSet>;
  rotated: boolean;
};

const ProfileTargetsList = ({ affected, skipped, rotated }: Props) => {
  const { t } = useTranslation('indexsets');

  return (
    <Container>
      <h5>
        <b>{t('field_types.targets_title')}</b>
      </h5>
      <Summary>
        {skipped.length > 0
          ? t('field_types.targets_summary_excluding', { count: affected.length, skipped: skipped.length })
          : t('field_types.targets_summary', { count: affected.length })}
      </Summary>
      <ScrollContainer $maxHeight={250}>
        <Table condensed>
          <thead>
            <tr>
              <th>{t('field_types.targets_index_set')}</th>
              <StatusHeader>{t('field_types.targets_status')}</StatusHeader>
              <th>{t('field_types.targets_note')}</th>
            </tr>
          </thead>
          <tbody>
            {affected.map(({ id, title, writable }) => (
              <tr key={id}>
                <td>{title}</td>
                <td>
                  <Badge color="success" variant="light">
                    {t('field_types.targets_included')}
                  </Badge>
                </td>
                <td>
                  {!writable && (
                    <Note>
                      {rotated ? t('field_types.targets_read_only_not_rotated') : t('field_types.targets_read_only')}
                    </Note>
                  )}
                </td>
              </tr>
            ))}
            {skipped.map(({ indexSet, reason }) => (
              <tr key={indexSet.id}>
                <td>{indexSet.title}</td>
                <td>
                  <Badge color="gray" variant="light">
                    {t('field_types.targets_excluded')}
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
};

export default ProfileTargetsList;
