// @flow strict
import * as React from 'react';
import { Jumbotron, Button } from 'components/graylog';

type Props = {|
  toggleEdit: () => void,
  editing: boolean,
|}

const EmptyAggregationContent = ({ toggleEdit, editing = false }: Props) => {
  const text = editing
    ? (
      <p>You are now editing the widget.<br />
        To see results, add at least one metric. You can group data by adding rows/columns.<br />
        To finish, click &quot;Save&quot; to save, &quot;Cancel&quot; to abandon changes.
      </p>
    )
    : (<p>Please <Button bsStyle="info" onClick={toggleEdit}>Edit</Button> the widget to see results here.</p>);
  return (
    <Jumbotron>
      <h2>Empty Aggregation</h2>
      <br />
      {text}
    </Jumbotron>
  );
};

export default EmptyAggregationContent;
