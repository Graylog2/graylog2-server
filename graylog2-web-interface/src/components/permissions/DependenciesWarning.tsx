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
import { Trans, useTranslation } from 'react-i18next';
import styled from 'styled-components';

import StringUtils from 'util/StringUtils';
import type { MissingDependencies, GranteesList } from 'logic/permissions/EntityShareState';
import { Alert } from 'components/bootstrap';
import useGranteeTypeLabel from 'components/permissions/useGranteeTypeLabel';

type Props = {
  missingDependencies: MissingDependencies;
  availableGrantees: GranteesList;
};

const Container = styled(Alert)`
  margin-top: 20px;
  max-height: 240px;
  overflow: auto;
`;

const List = styled.ul`
  list-style: initial;
  padding-left: 20px;

  ul {
    list-style: circle;
  }
`;

const _cap = StringUtils.capitalizeFirstLetter;

const DependenciesWarning = ({ missingDependencies, availableGrantees }: Props) => {
  const { t } = useTranslation('users');
  const granteeTypeLabel = useGranteeTypeLabel();

  return (
    <Container bsStyle="danger" title={t('permissions.dependencies_warning.title')}>
      <List>
        {missingDependencies
          .entrySeq()
          .map(([granteeGRN, dependencyList]) => {
            const grantee = availableGrantees.find((selectedGrantee) => selectedGrantee.id === granteeGRN);

            return (
              grantee && (
                <li key={grantee.id}>
                  <Trans
                    t={t}
                    i18nKey="permissions.dependencies_warning.needs_access"
                    values={{ type: granteeTypeLabel(grantee.type), title: grantee.title }}
                    components={{ granteeTitle: <i /> }}
                  />
                  {dependencyList.map((dependency) => (
                    <List key={dependency.id}>
                      <li>
                        <Trans
                          t={t}
                          i18nKey="permissions.dependencies_warning.dependency"
                          values={{ type: _cap(dependency.type), title: dependency.title }}
                          components={{ dependencyTitle: <i /> }}
                        />
                        <br />
                        {t('permissions.dependencies_warning.owners')}{' '}
                        {dependency.owners.map((owner, key) => (
                          <span key={owner.id}>
                            <Trans
                              t={t}
                              i18nKey="permissions.dependencies_warning.owner"
                              values={{ type: granteeTypeLabel(owner.type), title: owner.title }}
                              components={{ ownerTitle: <i /> }}
                            />
                            {key !== dependency.owners.size - 1 && ', '}
                          </span>
                        ))}
                      </li>
                    </List>
                  ))}
                </li>
              )
            );
          })
          .toArray()}
      </List>
    </Container>
  );
};

export default DependenciesWarning;
