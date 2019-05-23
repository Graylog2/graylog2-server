import React from 'react';
import PropTypes from 'prop-types';
import lodash from 'lodash';
import { ControlLabel, FormGroup, HelpBlock } from 'react-bootstrap';

import { MultiSelect } from 'components/common';
import { Input } from 'components/bootstrap';

import { naturalSortIgnoreCase } from 'util/SortUtils';
import FormsUtils from 'util/FormsUtils';

import commonStyles from '../common/commonStyles.css';

class FilterForm extends React.Component {
  static propTypes = {
    alertDefinition: PropTypes.object.isRequired,
    streams: PropTypes.array.isRequired,
    onChange: PropTypes.func.isRequired,
  };

  handleConfigChange = (event) => {
    const { alertDefinition, onChange } = this.props;
    const { name } = event.target;
    const config = lodash.cloneDeep(alertDefinition.config);
    config[name] = FormsUtils.getValueFromInput(event.target);
    onChange('config', config);
  };

  handleStreamsChange = (nextValue) => {
    const { alertDefinition, onChange } = this.props;
    const config = lodash.cloneDeep(alertDefinition.config);
    config.selected_streams = nextValue;
    onChange('config', config);
  };

  render() {
    const { alertDefinition, streams } = this.props;
    const formattedStreams = streams
      .map(stream => ({ label: stream.title, value: stream.id }))
      .sort((s1, s2) => naturalSortIgnoreCase(s1.label, s2.label));

    return (
      <fieldset>
        <h2 className={commonStyles.title}>Filter</h2>
        <Input id="filter-query"
               name="query"
               label="Search Query"
               type="text"
               help="Search query that Messages should match. You can use the same syntax as in the Search page."
               value={lodash.defaultTo(alertDefinition.config.query, '')}
               onChange={this.handleConfigChange} />

        <FormGroup>
          <ControlLabel>Streams <small className="text-muted">(Optional)</small></ControlLabel>
          <MultiSelect id="filter-streams"
                       matchProp="label"
                       onChange={selected => this.handleStreamsChange(selected === '' ? [] : selected.split(','))}
                       options={formattedStreams}
                       value={lodash.defaultTo(alertDefinition.config.selected_streams, []).join(',')} />
          <HelpBlock>Select streams the search should include. Searches in all streams if empty.</HelpBlock>
        </FormGroup>
      </fieldset>
    );
  }
}

export default FilterForm;
