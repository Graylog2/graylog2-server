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
import { useState } from 'react';
import styled from 'styled-components';
import { useTranslation } from 'react-i18next';

import { Button, Col, Row, Table } from 'components/bootstrap';
import { PaginatedList, NoSearchResult, NoEntitiesExist, SortIcon } from 'components/common';
import SidecarSearchForm from 'components/sidecars/common/SidecarSearchForm';

import SidecarFailureTrackingRows from './SidecarFailureTrackingRows';

import VerboseMessageModal from '../sidecars/VerboseMessageModal';
import type { Collector, PaginationInfo, SidecarSummary } from '../types';

const StyledSortIcon = styled(SortIcon)`
  && {
    width: 12px;
    margin-left: 5px;
    line-height: 1;
  }
`;

const StandardWidthCol = styled.col`
  width: 9%;
`;

const ErrorMessageCol = styled.col`
  width: 14%;
`;

const VerboseMessageCol = styled.col`
  width: 50%;
`;
type SortType = { field: string; order: string };
type Props = {
  sidecars: Array<SidecarSummary>;
  collectors: Array<Collector>;
  pagination: PaginationInfo;
  query: string;
  sort: SortType;
  onlyActive: boolean;
  onPageChange: (page: number, pageSize: number) => void;
  onQueryChange: (query?: string) => void;
  onSortChange: (sortField: string) => void;
  toggleShowInactive: () => void;
};

const NoMatchingListAlert = ({ onlyActive, query }: { onlyActive: boolean; query: string }) => {
  const { t } = useTranslation('sidecars');
  const showInactiveHint = onlyActive && t('failure_tracking.list.include_inactive_hint');

  return (
    <NoSearchResult>
      {t('failure_tracking.list.no_matching_sidecars', { query, inactiveHint: showInactiveHint })}
    </NoSearchResult>
  );
};

const SidecarTable = ({
  rows,
  sort,
  onSortChange,
}: {
  rows: React.ReactNode[];
  sort: SortType;
  onSortChange: (sortField: string) => void;
}) => {
  const { t } = useTranslation('sidecars');
  const columns = {
    node_name: t('failure_tracking.list.columns.sidecar'),
    collector: t('failure_tracking.list.columns.collector'),
    last_seen: t('failure_tracking.list.columns.last_seen'),
    'node_details.status.status': t('failure_tracking.list.columns.status'),
    message: t('failure_tracking.list.columns.error_message'),
    verbose_message: t('failure_tracking.list.columns.verbose_message'),
  };
  const sortableColumns = ['node_name', 'last_seen'];

  return (
    <Table responsive>
      <colgroup>
        <StandardWidthCol />
        <StandardWidthCol />
        <StandardWidthCol />
        <StandardWidthCol />
        <ErrorMessageCol />
        <VerboseMessageCol />
      </colgroup>
      <thead>
        <tr>
          {Object.keys(columns).map((columnKey) => (
            <th key={columnKey}>
              {columns[columnKey]}
              {sortableColumns.includes(columnKey) && (
                <StyledSortIcon
                  activeDirection={sort.field === columnKey ? sort.order : null}
                  onChange={() => onSortChange(columnKey)}
                  title={columnKey}
                  ascId="asc"
                  descId="desc"
                />
              )}
            </th>
          ))}
        </tr>
      </thead>
      <tbody>{rows}</tbody>
    </Table>
  );
};

const EmptyList = ({ query, onlyActive }: { query: string; onlyActive: boolean }) => {
  const { t } = useTranslation('sidecars');

  if (query) {
    return <NoMatchingListAlert onlyActive={onlyActive} query={query} />;
  }

  return <NoEntitiesExist>{t('failure_tracking.list.no_failures')}</NoEntitiesExist>;
};

const SidecarFailureTrackingList = ({
  sidecars,
  collectors,
  pagination,
  query,
  sort,
  onlyActive,
  onPageChange,
  onQueryChange,
  onSortChange,
  toggleShowInactive,
}: Props) => {
  const [collectorDetailsToShow, setCollectorDetailsToShow] = useState<{
    name: string;
    verbose_message: string;
  } | null>(null);
  const sidecarRows = sidecars.map((sidecar) => (
    <SidecarFailureTrackingRows
      key={sidecar.node_id}
      sidecar={sidecar}
      collectors={collectors}
      onShowDetails={setCollectorDetailsToShow}
    />
  ));
  const { t } = useTranslation('sidecars');

  return (
    <div>
      <div>
        <SidecarSearchForm query={query} onSearch={onQueryChange} onReset={onQueryChange}>
          <Button bsStyle="primary" onClick={toggleShowInactive}>
            {onlyActive
              ? t('failure_tracking.list.include_inactive_sidecars')
              : t('failure_tracking.list.hide_inactive_sidecars')}
          </Button>
        </SidecarSearchForm>
      </div>

      <PaginatedList showPageSizeSelect={false} totalItems={pagination.total} onChange={onPageChange}>
        <Row>
          <Col md={12}>
            {sidecarRows.length > 0 ? (
              <SidecarTable rows={sidecarRows} sort={sort} onSortChange={onSortChange} />
            ) : (
              <EmptyList query={query} onlyActive={onlyActive} />
            )}
          </Col>
        </Row>
      </PaginatedList>

      {collectorDetailsToShow && (
        <VerboseMessageModal
          showModal
          onHide={() => setCollectorDetailsToShow(null)}
          collectorName={collectorDetailsToShow.name}
          collectorVerbose={collectorDetailsToShow.verbose_message}
        />
      )}
    </div>
  );
};

export default SidecarFailureTrackingList;
