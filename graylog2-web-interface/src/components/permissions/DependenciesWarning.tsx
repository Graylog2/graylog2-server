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
import styled from 'styled-components';

import StringUtils from 'util/StringUtils';
import { MissingDependencies, GranteesList } from 'logic/permissions/EntityShareState';
import { Alert } from 'components/graylog';
import { ThemeInterface } from 'theme';

type Props = {
  missingDependencies: MissingDependencies,
  availableGrantees: GranteesList,
};

const Container = styled(Alert)`
  margin-top: 20px;
  max-height: 240px;
  overflow: auto;
`;

const Headline = styled.div`
  font-weight: bold;
  margin-bottom: 10px;
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
  return (
    <Container bsStyle="danger">
      <Headline>There are missing dependencies for the current set of collaborators</Headline>

      <List>
        {missingDependencies.entrySeq().map(([granteeGRN, dependencyList]) => {
          const grantee = availableGrantees.find((selectedGrantee) => selectedGrantee.id === granteeGRN);

          return (grantee && (
            <li key={grantee.id}>
              {_cap(grantee.type)} <i>{grantee.title}</i> needs access to
              {dependencyList.map((dependency) => (
                <List key={dependency.id}>
                  <li>
                    {_cap(dependency.type)}: <i>{dependency.title}</i><br />
                    Owners: {dependency.owners.map((owner, key) => (
                      <span key={owner.id}>
                        {_cap(owner.type)} <i>{owner.title}</i>
                        {key !== dependency.owners.size - 1 && ', '}
                      </span>
                    ))}
                  </li>
                </List>
              ))}
            </li>
          ));
        })}
      </List>
    </Container>
  );
};

export default DependenciesWarning;
