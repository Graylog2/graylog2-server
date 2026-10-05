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
import { useEffect, useRef, useState } from 'react';
import styled from 'styled-components';
import type { ColorVariant } from '@graylog/sawmill';

import Alert from 'components/bootstrap/Alert';
import Button from 'components/bootstrap/Button';
import ButtonToolbar from 'components/bootstrap/ButtonToolbar';
import Col from 'components/bootstrap/Col';
import DropdownButton from 'components/bootstrap/DropdownButton';
import Label from 'components/bootstrap/Label';
import MenuItem from 'components/bootstrap/menuitem/MenuItem';
import Row from 'components/bootstrap/Row';
import Table from 'components/bootstrap/Table';
import DocumentTitle from 'components/common/DocumentTitle';
import EntityFilters from 'components/common/EntityFilters/EntityFilters';
import useUrlQueryFilters from 'components/common/EntityFilters/hooks/useUrlQueryFilters';
import type { UrlQueryFilters } from 'components/common/EntityFilters/types';
import NoSearchResult from 'components/common/NoSearchResult';
import PageHeader from 'components/common/PageHeader';
import PageSizeSelect from 'components/common/PageSizeSelect';
import Pagination from 'components/common/Pagination';
import SearchForm from 'components/common/SearchForm';
import SortIcon from 'components/common/SortIcon';
import Spinner from 'components/common/Spinner';
import IndexerClusterHealth from 'components/indexers/IndexerClusterHealth';
import IndicesPageNavigation from 'components/indices/IndicesPageNavigation';
import useCurrentUser from 'hooks/useCurrentUser';
import Routes from 'routing/Routes';
import useHistory from 'routing/useHistory';
import NumberUtils from 'util/NumberUtils';
import { isPermitted } from 'util/PermissionsMixin';

import AllocationPanel from './allocation/AllocationPanel';
import RetryAllocationConfirm from './allocation/RetryAllocationConfirm';
import { useRetryFailedAllocations } from './allocation/useAllocation';
import { actionTargets, bulkMenu, canActOnAny, NOT_APPLICABLE_REASON, rowMenu } from './actions';
import { filterAttributes, matchesFilters, matchesQuery, pageOf, sortIndices } from './indexList';
import IndexActionConfirm from './IndexActionConfirm';
import type { CanRun, IndexAction, IndexSummary, Sort, SortField } from './types';
import useIndexActions from './useIndexActions';
import useIndexOverview from './useIndexOverview';

const HEALTH_STYLES: { [health: string]: ColorVariant } = { green: 'success', yellow: 'warning', red: 'danger' };
const TIER_LABELS: { [tier: string]: string } = { hot: 'Hot', warm: 'Warm' };
const TIER_STYLES: { [tier: string]: ColorVariant } = { hot: 'default', warm: 'info' };

const PAGE_SIZES = [10, 20, 50, 100];
const DEFAULT_PAGE_SIZE = 20;

type OnAction = (action: IndexAction, chosen: Array<IndexSummary>) => void;

const Toolbar = styled.div`
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
`;

const ToolbarEnd = styled(ButtonToolbar)`
  margin-left: auto;
`;

const CheckboxCell = styled.td`
  width: 30px;
`;

const NumberCell = styled.td`
  text-align: right;
  white-space: nowrap;
`;

// Header cells like core's EntityDataTable: the label, and its sort icon (on the label's inner side for right-aligned
// number columns).
const SortTh = styled.th<{ $alignRight: boolean }>`
  white-space: nowrap;
  text-align: ${({ $alignRight }) => ($alignRight ? 'right' : 'left')};
`;

const StyledSortIcon = styled(SortIcon)<{ $alignRight: boolean }>`
  display: inline-flex;
  padding: 0;
  ${({ $alignRight, theme }) =>
    $alignRight ? `margin-right: ${theme.spacings.xs};` : `margin-left: ${theme.spacings.xs};`}
`;

const SORT_ORDER_NAMES = { asc: 'ascending', desc: 'descending' } as const;

type SortHeaderProps = {
  field: SortField;
  label: string;
  sort: Sort;
  onSort: (field: SortField) => void;
  alignRight?: boolean;
};

const SortHeader = ({ field, label, sort, onSort, alignRight = false }: SortHeaderProps) => {
  const active = sort.field === field;
  const nextDirection = active && sort.direction === 'asc' ? 'desc' : 'asc';
  const icon = (
    <StyledSortIcon
      activeDirection={active ? sort.direction : null}
      onChange={() => onSort(field)}
      title={`Sort ${label.toLowerCase()} ${SORT_ORDER_NAMES[nextDirection]}`}
      ascId="asc"
      descId="desc"
      $alignRight={alignRight}
    />
  );

  return (
    <SortTh $alignRight={alignRight} aria-sort={active ? SORT_ORDER_NAMES[sort.direction] : 'none'}>
      {alignRight && icon}
      {label}
      {!alignRight && icon}
    </SortTh>
  );
};

// The row above the table: page size on the right, as in core's EntityDataTable. Bulk actions sit in the toolbar.
const ActionsRow = styled.div`
  display: flex;
  align-items: center;
  justify-content: flex-end;
  margin-bottom: 10px;
  min-height: 22px;
  width: 100%;
`;

// Below the table: the selection count on the left, page links centred.
const FooterRow = styled.div`
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
`;

const TierLabel = ({ tier }: { tier: IndexSummary['tier'] }) => (
  <Label bsStyle={TIER_STYLES[tier] ?? 'default'}>{TIER_LABELS[tier] ?? tier ?? 'unknown'}</Label>
);

const ActionsCell = styled.td`
  width: 1%;
  white-space: nowrap;
  text-align: right;
`;

const formatCount = (value: number | null) =>
  value === null || value === undefined ? '' : NumberUtils.formatNumber(value);
const formatBytes = (value: number | null) =>
  value === null || value === undefined ? '' : NumberUtils.formatBytes(value);

const HealthLabel = ({ index }: { index: IndexSummary }) => {
  if (index.status === 'close') {
    return <Label bsStyle="default">closed</Label>;
  }

  return <Label bsStyle={HEALTH_STYLES[index.health] ?? 'default'}>{index.health ?? 'unknown'}</Label>;
};

type SelectAllCheckboxProps = {
  checked: boolean;
  indeterminate: boolean;
  disabled: boolean;
  onChange: () => void;
};

// A checkbox that can show "some selected".
const SelectAllCheckbox = ({ checked, indeterminate, disabled, onChange }: SelectAllCheckboxProps) => {
  const ref = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (ref.current) {
      ref.current.indeterminate = indeterminate;
    }
  }, [indeterminate]);

  return (
    <input
      ref={ref}
      type="checkbox"
      checked={checked}
      disabled={disabled}
      onChange={onChange}
      aria-label="Select all shown indices"
      title="Select all shown indices"
    />
  );
};

// Only actions the user is permitted to run on this index; no menu at all if there are none.
const RowActions = ({ index, onAction, can }: { index: IndexSummary; onAction: OnAction; can: CanRun }) => {
  const items = rowMenu(index, can);

  if (items.length === 0) {
    return null;
  }

  return (
    <DropdownButton bsSize="xsmall" title="More" pullRight id={`index-actions-${index.index}`}>
      {items.map(({ action, applies }) => (
        <MenuItem
          key={action.key}
          disabled={!applies}
          title={applies ? undefined : NOT_APPLICABLE_REASON}
          onClick={() => onAction(action, [index])}>
          {action.label}
        </MenuItem>
      ))}
    </DropdownButton>
  );
};

type BulkActionsProps = {
  selected: Array<IndexSummary>;
  onAction: OnAction;
  onCancelSelection: () => void;
  can: CanRun;
};

// Like core's BulkActionsDropdown: "Bulk actions" with "Cancel selection" at the end of the menu. Lists only actions
// permitted on at least one selected index.
const BulkActions = ({ selected, onAction, onCancelSelection, can }: BulkActionsProps) => (
  <DropdownButton bsSize="small" title="Bulk actions" disabled={selected.length === 0} id="bulk-actions-dropdown">
      {bulkMenu(selected, can).map(({ action, applicable }) => (
        <MenuItem
          key={action.key}
          disabled={applicable === 0}
          title={applicable === 0 ? NOT_APPLICABLE_REASON : undefined}
          onClick={() => onAction(action, selected)}>
          {action.label}
          {applicable < selected.length && ` (${applicable} of ${selected.length})`}
        </MenuItem>
      ))}
      <MenuItem divider />
      <MenuItem onClick={onCancelSelection}>Cancel selection</MenuItem>
  </DropdownButton>
);

type IndexTableProps = {
  indices: Array<IndexSummary>;
  selectedNames: Set<string>;
  onToggle: (name: string) => void;
  onToggleAll: (select: boolean) => void;
  onAction: OnAction;
  sort: Sort;
  onSort: (field: SortField) => void;
  can: CanRun;
  selectable: boolean;
};

const IndexTable = ({
  indices,
  selectedNames,
  onToggle,
  onToggleAll,
  onAction,
  sort,
  onSort,
  can,
  selectable,
}: IndexTableProps) => {
  const selectedShown = indices.filter((index) => selectedNames.has(index.index)).length;

  return (
    <Table condensed hover striped>
      <thead>
        <tr>
          {selectable && (
            <th>
              <SelectAllCheckbox
                checked={indices.length > 0 && selectedShown === indices.length}
                indeterminate={selectedShown > 0 && selectedShown < indices.length}
                disabled={indices.length === 0}
                onChange={() => onToggleAll(selectedShown < indices.length)}
              />
            </th>
          )}
          <SortHeader field="index" label="Index" sort={sort} onSort={onSort} />
          <SortHeader field="health" label="Health" sort={sort} onSort={onSort} />
          <SortHeader field="tier" label="Tier" sort={sort} onSort={onSort} />
          <SortHeader field="index_set" label="Index set" sort={sort} onSort={onSort} />
          <SortHeader field="primary_shards" label="Primaries" sort={sort} onSort={onSort} alignRight />
          <SortHeader field="replicas" label="Replicas" sort={sort} onSort={onSort} alignRight />
          <SortHeader field="docs_count" label="Documents" sort={sort} onSort={onSort} alignRight />
          <SortHeader field="store_size_bytes" label="Size" sort={sort} onSort={onSort} alignRight />
          <th aria-label="Actions" />
        </tr>
      </thead>
      <tbody>
        {indices.map((index) => (
          <tr key={index.index}>
            {selectable && (
              <CheckboxCell>
                <input
                  type="checkbox"
                  checked={selectedNames.has(index.index)}
                  onChange={() => onToggle(index.index)}
                  aria-label={`Select ${index.index}`}
                />
              </CheckboxCell>
            )}
            <td>
              {index.index} {index.is_write_index && <Label bsStyle="info">write index</Label>}
            </td>
            <td>
              <HealthLabel index={index} />
            </td>
            <td>
              <TierLabel tier={index.tier} />
            </td>
            <td>{index.index_set_title ?? <i>not managed by Graylog</i>}</td>
            <NumberCell>{formatCount(index.primary_shards)}</NumberCell>
            <NumberCell>{formatCount(index.replicas)}</NumberCell>
            <NumberCell>{formatCount(index.docs_count)}</NumberCell>
            <NumberCell>{formatBytes(index.store_size_bytes)}</NumberCell>
            <ActionsCell>
              <RowActions index={index} onAction={onAction} can={can} />
            </ActionsCell>
          </tr>
        ))}
      </tbody>
    </Table>
  );
};

type PendingAction = {
  action: IndexAction;
  targets: Array<IndexSummary>;
  skipped: number;
};

const IndexManagementContent = () => {
  const { indices, error, isLoading, isFetching, refetch } = useIndexOverview();
  const { runAction, isRunning } = useIndexActions();
  const { retryFailed, isRetrying } = useRetryFailedAllocations();
  const [query, setQuery] = useState('');
  const [filters, setFilters] = useUrlQueryFilters();
  const [selectedNames, setSelectedNames] = useState<Set<string>>(new Set());
  const [pending, setPending] = useState<PendingAction | undefined>(undefined);
  const [showAllocation, setShowAllocation] = useState(false);
  const [confirmRetry, setConfirmRetry] = useState(false);
  const [sort, setSort] = useState<Sort>({ field: 'index', direction: 'asc' });
  const [pagination, setPagination] = useState({ page: 1, pageSize: DEFAULT_PAGE_SIZE });
  const currentUser = useCurrentUser();

  // Controls follow the permissions the server checks: per index for actions, unscoped indices:changestate for
  // retrying allocations, indexercluster:read for explaining them.
  const can: CanRun = (action, index) => isPermitted(currentUser.permissions, action.permission(index));
  const canExplain = isPermitted(currentUser.permissions, 'indexercluster:read');
  const canRetry = isPermitted(currentUser.permissions, 'indices:changestate');
  const selectable = canActOnAny(indices, can);

  // A different filter, search or sort starts over on page 1.
  const toFirstPage = () => setPagination((current) => ({ ...current, page: 1 }));

  const changeQuery = (newQuery: string) => {
    setQuery(newQuery);
    toFirstPage();
  };

  const changeFilters = (newFilters: UrlQueryFilters) => {
    setFilters(newFilters);
    toFirstPage();
  };

  // Same column again flips the direction; a new column starts ascending.
  const changeSort = (field: SortField) => {
    setSort((current) =>
      current.field === field
        ? { field, direction: current.direction === 'asc' ? 'desc' : 'asc' }
        : { field, direction: 'asc' },
    );
    toFirstPage();
  };

  // From the allocation panel: narrow the list to one index.
  const showIndex = (name: string) => {
    changeFilters(filters.clear());
    changeQuery(name);
  };

  const confirmRetryFailed = async () => {
    try {
      await retryFailed();
    } finally {
      setConfirmRetry(false);
    }
  };

  const attributes = filterAttributes(indices);
  const visible = sortIndices(
    indices.filter((index) => matchesFilters(index, filters) && matchesQuery(index, query)),
    sort,
  );
  const { totalPages, currentPage, rows: pageRows } = pageOf(visible, pagination.page, pagination.pageSize);

  // Selected indices that still exist (deleted ones drop out after a refresh).
  const selected = indices.filter((index) => selectedNames.has(index.index));

  const toggle = (name: string) =>
    setSelectedNames((current) => {
      const next = new Set(current);

      if (next.has(name)) {
        next.delete(name);
      } else {
        next.add(name);
      }

      return next;
    });

  // Select or deselect the rows on the current page; selections on other pages or hidden by the filter are kept.
  const toggleAll = (select: boolean) =>
    setSelectedNames((current) => {
      const next = new Set(current);
      pageRows.forEach((index) => (select ? next.add(index.index) : next.delete(index.index)));

      return next;
    });

  const requestAction: OnAction = (action, chosen) => {
    const targets = actionTargets(action, chosen, can);
    setPending({ action, targets, skipped: chosen.length - targets.length });
  };

  const confirmAction = async () => {
    const { action, targets } = pending;

    try {
      await runAction({ action, indices: targets.map((index) => index.index) });
      setSelectedNames(new Set());
    } finally {
      setPending(undefined);
    }
  };

  if (isLoading) {
    return <Spinner text="Loading indices..." />;
  }

  if (error) {
    return <Alert bsStyle="danger">Couldn&apos;t load indices: {error.message}</Alert>;
  }

  return (
    <>
      {showAllocation && (
        <AllocationPanel
          onClose={() => setShowAllocation(false)}
          onShowIndex={showIndex}
          onRetry={canRetry ? () => setConfirmRetry(true) : undefined}
        />
      )}
      <Toolbar>
        <SearchForm
          query={query}
          onQueryChange={changeQuery}
          onSearch={changeQuery}
          onReset={() => changeQuery('')}
          placeholder="Filter by index or index set"
          topMargin={0}>
          {/* As core's PaginatedEntityTable places its filters: inside the search form. */}
          <div style={{ marginBottom: 5 }}>
            <EntityFilters
              attributes={attributes}
              urlQueryFilters={filters}
              setUrlQueryFilters={changeFilters}
              appSection="index-management"
            />
          </div>
        </SearchForm>
        <ToolbarEnd>
          {canExplain && (
            <Button
              bsSize="small"
              onClick={() => setShowAllocation(true)}
              disabled={showAllocation}
              title="Explain why shards are unassigned (_cluster/allocation/explain)">
              Explain allocation
            </Button>
          )}
          {canRetry && (
            <Button
              bsSize="small"
              onClick={() => setConfirmRetry(true)}
              title="Retry shards OpenSearch gave up on (_cluster/reroute?retry_failed=true)">
              Retry failed allocations
            </Button>
          )}
          {selectable && (
            <BulkActions
              selected={selected}
              onAction={requestAction}
              onCancelSelection={() => setSelectedNames(new Set())}
              can={can}
            />
          )}
          <Button bsSize="small" onClick={() => refetch()} disabled={isFetching}>
            {isFetching ? 'Refreshing...' : 'Refresh'}
          </Button>
        </ToolbarEnd>
      </Toolbar>
      {visible.length === 0 ? (
        <NoSearchResult>No indices match the filter.</NoSearchResult>
      ) : (
        <>
          <ActionsRow>
            <PageSizeSelect
              pageSize={pagination.pageSize}
              pageSizes={PAGE_SIZES}
              onChange={(pageSize) => setPagination({ page: 1, pageSize })}
            />
          </ActionsRow>
          <IndexTable
            indices={pageRows}
            selectedNames={selectedNames}
            onToggle={toggle}
            onToggleAll={toggleAll}
            onAction={requestAction}
            sort={sort}
            onSort={changeSort}
            can={can}
            selectable={selectable}
          />
          <FooterRow>
            <div>
              {selected.length > 0 && `${selected.length} ${selected.length === 1 ? 'item' : 'items'} selected`}
            </div>
            <div className="pagination-wrapper">
              <Pagination
                totalPages={totalPages}
                currentPage={currentPage}
                onChange={(page) => setPagination((current) => ({ ...current, page }))}
              />
            </div>
          </FooterRow>
        </>
      )}
      {pending && (
        <IndexActionConfirm
          action={pending.action}
          targets={pending.targets}
          skipped={pending.skipped}
          isRunning={isRunning}
          onConfirm={confirmAction}
          onCancel={() => setPending(undefined)}
        />
      )}
      {confirmRetry && (
        <RetryAllocationConfirm
          isRetrying={isRetrying}
          onConfirm={confirmRetryFailed}
          onCancel={() => setConfirmRetry(false)}
        />
      )}
    </>
  );
};

// As ClusterConfigurationPage: the route itself isn't permission-gated, so the page checks for itself.
const IndexManagementPage = () => {
  const currentUser = useCurrentUser();
  const canRead = isPermitted(currentUser.permissions, 'indices:read');
  const { push } = useHistory();

  // Without the permission the page doesn't exist for the user.
  useEffect(() => {
    if (!canRead) {
      push(Routes.NOTFOUND);
    }
  }, [canRead, push]);

  if (!canRead) {
    return null;
  }

  return (
    <DocumentTitle title="Index Management">
      <IndicesPageNavigation />
      <PageHeader title="Index Management">
        <span>
          Every index in the cluster with its health, including hidden indices and indices that don&apos;t belong to a
          Graylog index set. Refreshes every 30 seconds.
        </span>
      </PageHeader>
      <IndexerClusterHealth minimal />
      <Row className="content">
        <Col md={12}>
          <IndexManagementContent />
        </Col>
      </Row>
    </DocumentTitle>
  );
};

export default IndexManagementPage;
