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
import styled from 'styled-components';
import { withTranslation } from 'react-i18next';
import type { WithTranslation } from 'react-i18next';

import { Button, Col, Row, Table } from 'components/bootstrap';
import { Icon, SortIcon, PaginatedList, NoSearchResult, NoEntitiesExist } from 'components/common';
import SidecarSearchForm from 'components/sidecars/common/SidecarSearchForm';

import SidecarRow from './SidecarRow';
import style from './SidecarList.css';

const StyledSortIcon = styled(SortIcon)`
  && {
    width: 12px;
    margin-left: 5px;
    line-height: 1;
  }
`;

export const PAGE_SIZES = [10, 25, 50, 100];

type SidecarListProps = {
  sidecars: any[];
  onlyActive: boolean | string;
  pagination: any;
  query: string;
  sort: any;
  onPageChange: (...args: any[]) => void;
  onQueryChange: (...args: any[]) => void;
  onSortChange: (...args: any[]) => (newDirection: string) => void;
  toggleShowInactive: (...args: any[]) => void;
} & WithTranslation;

class SidecarList extends React.Component<
  SidecarListProps,
  {
    [key: string]: any;
  }
> {
  formatSidecarList = (sidecars: React.ReactElement[]) => {
    const { onSortChange, sort, t } = this.props;
    const sidecarCollection = {
      node_name: t('sidecars.sidecar_list.name'),
      'node_details.status.status': t('sidecars.sidecar_list.status'),
      'node_details.operating_system': t('sidecars.sidecar_list.operating_system'),
      last_seen: t('sidecars.sidecar_list.last_seen'),
      node_id: t('sidecars.sidecar_list.node_id'),
      sidecar_version: t('sidecars.sidecar_list.sidecar_version'),
    };

    return (
      <Table responsive className={style.sidecarList}>
        <thead>
          <tr>
            {Object.keys(sidecarCollection).map((sidecar) => (
              <th key={sidecar}>
                {sidecarCollection[sidecar]}
                <StyledSortIcon
                  activeDirection={sort.field === sidecar ? sort.order : null}
                  onChange={onSortChange(sidecar)}
                  ascId="asc"
                  descId="desc"
                />
              </th>
            ))}
            <th className={style.actions}>&nbsp;</th>
          </tr>
        </thead>
        <tbody>{sidecars}</tbody>
      </Table>
    );
  };

  formatNoMatchingListAlert = () => {
    const { onlyActive, t } = this.props;
    const showInactiveHint = onlyActive ? t('sidecars.sidecar_list.include_inactive_hint') : '';

    return (
      <NoSearchResult>
        <Icon name="info" />
        &nbsp;{t('sidecars.sidecar_list.no_matching_sidecars', { inactiveHint: showInactiveHint })}
      </NoSearchResult>
    );
  };

  renderEmptyList = () => {
    const { query, t } = this.props;

    if (query) {
      return this.formatNoMatchingListAlert();
    }

    return <NoEntitiesExist>{t('sidecars.sidecar_list.no_sidecars')}</NoEntitiesExist>;
  };

  render() {
    const { sidecars, onlyActive, pagination, query, onQueryChange, onPageChange, toggleShowInactive, t } = this.props;
    const sidecarRows = sidecars.map((sidecar) => <SidecarRow key={sidecar.node_id} sidecar={sidecar} />);
    const sidecarList = sidecarRows.length > 0 ? this.formatSidecarList(sidecarRows) : this.renderEmptyList();

    return (
      <div>
        <div className={style.sidecarsFilter}>
          <SidecarSearchForm query={query} onSearch={onQueryChange} onReset={onQueryChange}>
            <Button bsStyle="primary" onClick={toggleShowInactive} className={style.inactiveSidecarsButton}>
              {onlyActive
                ? t('sidecars.sidecar_list.include_inactive_sidecars')
                : t('sidecars.sidecar_list.hide_inactive_sidecars')}
            </Button>
          </SidecarSearchForm>
        </div>

        <PaginatedList pageSizes={PAGE_SIZES} totalItems={pagination.total} onChange={onPageChange}>
          <Row>
            <Col md={12}>{sidecarList}</Col>
          </Row>
        </PaginatedList>
      </div>
    );
  }
}

export default withTranslation('sidecars')(SidecarList);
