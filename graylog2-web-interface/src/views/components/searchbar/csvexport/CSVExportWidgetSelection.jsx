// @flow strict
import * as React from 'react';
import { List } from 'immutable';

import Widget from 'views/logic/widgets/Widget';
import View from 'views/logic/views/View';

import { Row, Alert } from 'components/graylog';
import IfDashboard from 'views/components/dashboard/IfDashboard';
import IfSearch from 'views/components/search/IfSearch';
import Select from 'views/components/Select';

type WidgetSelectionProps = {
  selectWidget: {label: string, value: Widget} => void,
  widgets: List<Widget>,
  view: View,
};

const WidgetSelection = ({ selectWidget, widgets, view }: WidgetSelectionProps) => {
  const widgetOption = (widget) => ({ label: view.getWidgetTitleByWidget(widget), value: widget });
  const widgetOptions = widgets.map((widget) => (widgetOption(widget))).toArray();
  return (
    <>
      <Row>
        <IfSearch>
          The CSV file will contain all messages for your current search.<br />
          Please select a message table to adopt its fields. You can adjust all settings in the next step.
        </IfSearch>
        <IfDashboard>
          Please select the message table you want to export the search results for. You can adjust its fields in the next step.<br />
          Selecting a message table equals using the option &quot;Export to CSV&quot; in a message table action menu.
        </IfDashboard>
      </Row>
      {widgets.size !== 0 ? (
        <Row>
          <label htmlFor="widget-selection">Select message table</label>
          <Select placeholder="Select message table"
                  onChange={selectWidget}
                  options={widgetOptions}
                  inputId="widget-selection" />
        </Row>
      ) : (
        <Row>
          <Alert bsStyle="warning">You need to create a message table widget to export its result.</Alert>
        </Row>
      )}
    </>
  );
};


export default WidgetSelection;
