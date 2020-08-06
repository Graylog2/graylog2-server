// @flow strict
import * as React from 'react';
import styled, { type StyledComponent } from 'styled-components';

import type { ThemeInterface } from 'theme';
import { SearchForm, Icon } from 'components/common';
import { OverlayTrigger, Popover, Table, Button } from 'components/graylog';

type Props = {
  onSearch: (query: string) => Promise<void>,
  onReset: () => Promise<void>,
};

const Container: StyledComponent<{}, ThemeInterface, HTMLDivElement> = styled.div`
  margin-bottom: 10px;
`;

const rolesQueryHelperPopover = (
  <Popover id="role-search-query-help" title="Search Syntax Help">
    <p><strong>Available search fields</strong></p>
    <Table condensed>
      <thead>
        <tr>
          <th>Field</th>
          <th>Description</th>
        </tr>
      </thead>
      <tbody>
        <tr>
          <td>name</td>
          <td>The name of a role</td>
        </tr>
        <tr>
          <td>description</td>
          <td>The description of a role</td>
        </tr>
      </tbody>
    </Table>
    <p><strong>Examples</strong></p>
    <p>
      Find role with a name containing manager:<br />
      <kbd>name:manager</kbd><br />
    </p>
  </Popover>
);

const rolesQueryHelper = (
  <OverlayTrigger trigger="click" rootClose placement="right" overlay={rolesQueryHelperPopover}>
    <Button bsStyle="link"><Icon name="question-circle" /></Button>
  </OverlayTrigger>
);

const RolesFilter = ({ onSearch, onReset }: Props) => {
  const _handleSearch = (newQuery, resetLoading) => onSearch(newQuery).then(resetLoading);

  return (
    <Container>
      <SearchForm onSearch={_handleSearch}
                  onReset={onReset}
                  useLoadingState
                  queryHelpComponent={rolesQueryHelper}
                  topMargin={0} />
    </Container>
  );
};

export default RolesFilter;
