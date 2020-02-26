// @flow strict
import React, { useCallback, useState } from 'react';
import URI from 'urijs';
import history from 'util/History';

import { Button, Checkbox, ControlLabel, FormGroup, HelpBlock, MenuItem, Modal } from 'components/graylog';
import Input from 'components/bootstrap/Input';
import { Icon } from 'components/common';
import Routes from 'routing/Routes';
import View from 'views/logic/views/View';
import queryTitle from 'views/logic/queries/QueryTitle';

export type UntypedBigDisplayModeQuery = {|
  tabs?: string,
  interval?: string,
  refresh?: string,
|};

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
    queryTitle(view, query.id),
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
      <form onSubmit={_onSave} data-testid="display-mode-config-form">
        <Modal.Header closeButton>
          <Modal.Title>Configuring Full Screen</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <Input id="refresh-interval"
                 type="number"
                 min="1"
                 name="refresh-interval"
                 label="Refresh Interval"
                 help="After how many seconds should the dashboard refresh?"
                 onChange={({ target: { value } }) => setRefreshInterval(value ? Number.parseInt(value, 10) : value)}
                 required
                 step="1"
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
                 min="1"
                 name="query-cycle-interval"
                 label="Tab cycle interval"
                 help="After how many seconds should the next tab be shown?"
                 onChange={({ target: { value } }) => setQueryCycleInterval(value ? Number.parseInt(value, 10) : value)}
                 required
                 step="1"
                 value={queryCycleInterval} />
        </Modal.Body>
        <Modal.Footer>
          <Button bsStyle="success" type="submit">Save</Button>
          <Button onClick={onCancel}>Cancel</Button>
        </Modal.Footer>
      </form>
    </Modal>
  );
};

const redirectToBigDisplayMode = (view: View, config: UntypedBigDisplayModeQuery): void => history.push(
  new URI(Routes.pluginRoute('DASHBOARDS_TV_VIEWID')(view.id))
    .search(config)
    .toString(),
);

const createQueryFromConfiguration = (
  { queryCycleInterval, queryTabs, refreshInterval }: Configuration,
  view: View,
): UntypedBigDisplayModeQuery => {
  const basicConfiguration = {
    interval: Number(queryCycleInterval).toString(),
    refresh: Number(refreshInterval).toString(),
  };
  const allQueryIndices = view.search.queries.toIndexedSeq().map((_, v) => v).toJS();
  return !queryTabs || allQueryIndices.join(',') === queryTabs.join(',')
    ? basicConfiguration
    : { ...basicConfiguration, tabs: queryTabs.join(',') };
};

type Props = {
  disabled?: boolean,
  view: View,
  show?: boolean
};

const BigDisplayModeConfiguration = ({ disabled, view, show }: Props) => {
  const [showConfigurationModal, setShowConfigurationModal] = useState(show);
  const onCancel = useCallback(() => setShowConfigurationModal(false), [setShowConfigurationModal]);

  const onSave = (config: Configuration) => redirectToBigDisplayMode(view, createQueryFromConfiguration(config, view));

  return (
    <React.Fragment>
      {showConfigurationModal && <ConfigurationModal view={view} onCancel={onCancel} onSave={onSave} />}
      <MenuItem disabled={disabled} onSelect={() => setShowConfigurationModal(true)}>
        <Icon name="desktop" /> Full Screen
      </MenuItem>
    </React.Fragment>
  );
};

BigDisplayModeConfiguration.defaultProps = {
  disabled: false,
  show: false,
};

export default BigDisplayModeConfiguration;
