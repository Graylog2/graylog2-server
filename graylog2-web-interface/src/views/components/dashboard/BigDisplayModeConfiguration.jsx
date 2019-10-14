// @flow strict
import React, { useCallback, useState } from 'react';
import URI from 'urijs';
import history from 'util/History';

import { Button, Checkbox, ControlLabel, FormGroup, HelpBlock, MenuItem, Modal } from 'components/graylog';
import Input from 'components/bootstrap/Input';
import Routes from 'routing/Routes';
import View from 'views/logic/views/View';
import type { BigDisplayModeQuery } from 'views/pages/ShowDashboardInBigDisplayMode';
import queryTitle from 'views/logic/queries/QueryTitle';

type Configuration = {
  refreshInterval: number,
  cycleTabs: boolean,
  queryTabs: Array<number>,
  queryCycleInterval: number,
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

  const multipleTabs = view.search.queries.size > 1;
  const [refreshInterval, setRefreshInterval] = useState(10);
  const [cycleTabs, setCycleTabs] = useState(multipleTabs);
  const [queryTabs, setQueryTabs] = useState(availableTabs.map(([idx]) => idx));
  const [queryCycleInterval, setQueryCycleInterval] = useState(30);
  const addQueryTab = useCallback(idx => setQueryTabs([...queryTabs, idx]), [queryTabs, setQueryTabs]);
  const removeQueryTab = useCallback(idx => setQueryTabs(queryTabs.filter(tab => tab !== idx)), [queryTabs, setQueryTabs]);
  const _onSave = useCallback(() => onSave({
    refreshInterval,
    cycleTabs,
    queryTabs,
    queryCycleInterval,
  }), [onSave]);

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
               onChange={setRefreshInterval}
               value={refreshInterval} />
        <Input id="cycle-tabs"
               type="checkbox"
               name="cycle-tabs"
               label="Cycle Tabs?"
               help="Should we cycle through existing tabs?"
               onChange={event => setCycleTabs(event.target.checked)}
               checked={cycleTabs} />
        <Input id="query-cycle-interval"
               disabled={!cycleTabs}
               type="number"
               name="query-cycle-interval"
               label="Tab cycle interval"
               help="After how many seconds should the next tab be shown?"
               onChange={setQueryCycleInterval}
               value={queryCycleInterval} />

        <FormGroup>
          <ControlLabel>Widgets</ControlLabel>
          <HelpBlock>
            Select the query tabs to include.
          </HelpBlock>
        </FormGroup>

        <ul>
          {availableTabs.map(([idx, title]) => (
            <li key={`${idx}-${title}`}>
              <Checkbox inline
                        disabled={cycleTabs !== true}
                        checked={queryTabs.includes(idx)}
                        onChange={event => (event.target.checked ? addQueryTab(idx) : removeQueryTab(idx))}>
                {title}
              </Checkbox>
            </li>
          ))}
        </ul>

      </Modal.Body>
      <Modal.Footer>
        <Button onClick={_onSave} bsStyle="success">Save</Button>
        <Button onClick={onCancel}>Cancel</Button>
      </Modal.Footer>
    </Modal>
  );
};

const redirectToBigDisplayMode = (view: View, config: BigDisplayModeQuery): void => history.push(
  new URI(Routes.pluginRoute('DASHBOARDS_TV_VIEWID')(view.id))
    .search(config)
    .toString(),
);

const createQueryFromConfiguration = (config: Configuration): BigDisplayModeQuery => ({
  cycle: config.cycleTabs,
  interval: config.queryCycleInterval,
  tabs: config.queryTabs,
  refresh: config.refreshInterval,
});

const BigDisplayModeConfiguration = ({ view }: { view: View }) => {
  const [showConfigurationModal, setShowConfigurationModal] = useState(false);
  const onCancel = useCallback(() => setShowConfigurationModal(false), [setShowConfigurationModal]);

  const onSave = (config: Configuration) => redirectToBigDisplayMode(view, createQueryFromConfiguration(config));

  return (
    <React.Fragment>
      {showConfigurationModal && <ConfigurationModal view={view} onCancel={onCancel} onSave={onSave} />}
      <MenuItem onSelect={() => setShowConfigurationModal(true)}><i className="fa fa-desktop" /> Full Screen</MenuItem>
    </React.Fragment>
  );
};

export default BigDisplayModeConfiguration;
