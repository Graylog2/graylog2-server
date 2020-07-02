// @flow strict
import * as React from 'react';
import styled, { type StyledComponent } from 'styled-components';

import StringUtils from 'util/StringUtils';
import { type MissingDependencies, type SelectedGrantees } from 'logic/permissions/EntityShareState';
import { Alert } from 'components/graylog';
import { type ThemeInterface } from 'theme';

type Props = {
  missingDependencies: MissingDependencies,
  selectedGrantees: SelectedGrantees,
};

const Container: StyledComponent<{}, ThemeInterface, Alert> = styled(Alert)`
  margin-top: 20px;
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

const DependenciesWarning = ({ missingDependencies, selectedGrantees }: Props) => {
  return (
    <Container bsStyle="danger">
      <Headline>There are missing dependecies for the current set of collaborators</Headline>

      <List>
        {missingDependencies.entrySeq().map(([granteeGRN, dependencyList]) => {
          const grantee = selectedGrantees.find((selectedGrantee) => selectedGrantee.id === granteeGRN);

          return (grantee && (
            <li key={grantee.id}>
              {_cap(grantee.type)} {grantee.title} needs access to
              {dependencyList.map((dependecy) => (
                <List key={dependecy.id}>
                  <li>
                    {_cap(dependecy.type)}: {dependecy.title}<br />
                    Owners: {dependecy.owners.map((owner, key) => (
                      <span key={owner.id}>
                        {_cap(owner.type)} {owner.title}
                        {key !== dependecy.owners.size - 1 && ', '}
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
