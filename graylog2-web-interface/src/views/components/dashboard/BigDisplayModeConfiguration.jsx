// @flow strict
import React, { useCallback, useState } from 'react';
import { Button, Checkbox, ControlLabel, FormGroup, HelpBlock, MenuItem, Modal } from 'components/graylog';
import Input from 'components/bootstrap/Input';
import View from 'views/logic/views/View';

const ConfigurationModal = ({ onSave, onCancel, view }: { view: View }) => {
  const availableTabs = view.search.queries.keySeq().map((q, idx) => [idx, view.state.getIn([q.id]).titles.getIn(['tab', 'title'], `Query#${idx}`)]).toJS();

  const [refreshInterval, setRefreshInterval] = useState(10);
  const [cycleTabs, setCycleTabs] = useState(true);
  const [queryTabs, setQueryTabs] = useState(availableTabs.map(([idx, _]) => idx));
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

const BigDisplayMode = ({ view }) => {
  const [showConfigurationModal, setShowConfigurationModal] = useState(false);
  const onCancel = useCallback(() => setShowConfigurationModal(false), [setShowConfigurationModal]);

  const onSave = (v) => { console.log(v); onCancel(); };

  return (
    <React.Fragment>
      {showConfigurationModal && <ConfigurationModal view={view} onCancel={onCancel} onSave={onSave} />}
      <MenuItem onSelect={() => setShowConfigurationModal(true)}><i className="fa fa-desktop" /> Full Screen</MenuItem>
    </React.Fragment>
  );
};

BigDisplayMode.propTypes = {};

export default BigDisplayMode;
