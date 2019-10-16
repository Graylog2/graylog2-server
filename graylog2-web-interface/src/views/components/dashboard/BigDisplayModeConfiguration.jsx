// @flow strict
import React, { useCallback, useState } from 'react';
import URI from 'urijs';
import history from 'util/History';

import { Button, Checkbox, ControlLabel, FormGroup, HelpBlock, MenuItem, Modal } from 'components/graylog';
import Input from 'components/bootstrap/Input';
import Routes from 'routing/Routes';
import View from 'views/logic/views/View';
import queryTitle from 'views/logic/queries/QueryTitle';
import type { UntypedBigDisplayModeQuery } from 'views/pages/ShowDashboardInBigDisplayMode';

type Configuration = {
  refreshInterval: number,
  queryTabs?: Array<number>,
  queryCycleInterval?: number,
};

type ConfigurationModalProps = {
  onSave: (Configuration) => void,
  onCancel: () => void,
  view: View,
};

const ConfigurationModal = ({ onSave, onCancel, view }: ConfigurationModalProps) => {
  const availableTabs = view.search.queries.keySeq().map((query, idx) => [
    idx,
    queryTitle(view, query),
  ]).toJS();

  const [refreshInterval, setRefreshInterval] = useState(10);
  const [queryTabs, setQueryTabs] = useState(availableTabs.map(([idx]) => idx));
  const [queryCycleInterval, setQueryCycleInterval] = useState(30);
  const addQueryTab = useCallback(idx => setQueryTabs([...queryTabs, idx]), [queryTabs, setQueryTabs]);
  const removeQueryTab = useCallback(idx => setQueryTabs(queryTabs.filter(tab => tab !== idx)), [queryTabs, setQueryTabs]);
  const _onSave = useCallback(() => onSave({
    refreshInterval,
    queryTabs,
    queryCycleInterval,
  }), [onSave, refreshInterval, queryTabs, queryCycleInterval]);

  return (
    <Modal show bsSize="large" onHide={onCancel}>
      <Modal.Header closeButton>
        <Modal.Title>Configuring Full Screen</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <Input id="refresh-interval"
               type="number"
               name="refresh-interval"
               label="Refresh Interval"
               help="After how many seconds should the dashboard refresh?"
               onChange={({ target: { value } }) => setRefreshInterval(Number.parseInt(value, 10))}
               required
               value={refreshInterval} />

        <FormGroup>
          <ControlLabel>Tabs</ControlLabel>
          <ul>
            {availableTabs.map(([idx, title]) => (
              <li key={`${idx}-${title}`}>
                <Checkbox inline
                          checked={queryTabs.includes(idx)}
                          onChange={event => (event.target.checked ? addQueryTab(idx) : removeQueryTab(idx))}>
                  {title}
                </Checkbox>
              </li>
            ))}
          </ul>
          <HelpBlock>
            Select the query tabs to include in rotation.
          </HelpBlock>
        </FormGroup>

        <Input id="query-cycle-interval"
               type="number"
               name="query-cycle-interval"
               label="Tab cycle interval"
               help="After how many seconds should the next tab be shown?"
               onChange={({ target: { value } }) => setQueryCycleInterval(value)}
               required
               value={queryCycleInterval} />
      </Modal.Body>
      <Modal.Footer>
        <Button onClick={_onSave} bsStyle="success">Save</Button>
        <Button onClick={onCancel}>Cancel</Button>
      </Modal.Footer>
    </Modal>
  );
};

const redirectToBigDisplayMode = (view: View, config: UntypedBigDisplayModeQuery): void => history.push(
  new URI(Routes.pluginRoute('DASHBOARDS_TV_VIEWID')(view.id))
    .search(config)
    .toString(),
);

const createQueryFromConfiguration = ({ queryCycleInterval, queryTabs, refreshInterval }: Configuration): UntypedBigDisplayModeQuery => ({
  interval: Number(queryCycleInterval).toString(),
  tabs: queryTabs !== undefined ? queryTabs.join(',') : undefined,
  refresh: Number(refreshInterval).toString(),
});

type Props = {
  disabled?: boolean,
  view: View,
  show?: boolean
};

const BigDisplayModeConfiguration = ({ disabled, view, show }: Props) => {
  const [showConfigurationModal, setShowConfigurationModal] = useState(show);
  const onCancel = useCallback(() => setShowConfigurationModal(false), [setShowConfigurationModal]);

  const onSave = (config: Configuration) => redirectToBigDisplayMode(view, createQueryFromConfiguration(config));

  return (
    <React.Fragment>
      {showConfigurationModal && <ConfigurationModal view={view} onCancel={onCancel} onSave={onSave} />}
      <MenuItem disabled={disabled} onSelect={() => setShowConfigurationModal(true)}>
        <i className="fa fa-desktop" /> Full Screen
      </MenuItem>
    </React.Fragment>
  );
};

BigDisplayModeConfiguration.defaultProps = {
  disabled: false,
  show: false,
};

export default BigDisplayModeConfiguration;
