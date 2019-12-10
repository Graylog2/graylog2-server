// @flow strict
import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { groupBy } from 'lodash';

import Select from 'components/common/Select';
import DecoratorList from 'views/components/messagelist/decorators/DecoratorList';
import { defaultCompare } from 'views/logic/DefaultCompare';
import SelectContainer from './SelectContainer';
import Decorator from '../../../views/components/messagelist/decorators/Decorator';

const DEFAULT_STREAM_ID = '000000000000000000000001';
const DEFAULT_SEARCH_ID = 'DEFAULT_SEARCH';

const _formatDecorator = (decorator, decorators, decoratorTypes, updateFn) => {
  const typeDefinition = decoratorTypes[decorator.type] || { requested_configuration: {}, name: `Unknown type: ${decorator.type}` };
  const onUpdate = updateFn
    ? (id, updatedDecorator) => updateFn(decorators.map(curDecorator => (curDecorator.id === id ? updatedDecorator : curDecorator)))
    : () => {};
  const onDelete = updateFn
    ? deletedDecoratorId => updateFn(decorators.filter(({ id }) => (id !== deletedDecoratorId)))
    : () => {};
  return ({
    id: decorator.id,
    title: <Decorator key={`decorator-${decorator.id}`}
                      decorator={decorator}
                      decoratorTypes={decoratorTypes}
                      disableMenu={updateFn === undefined}
                      onUpdate={onUpdate}
                      onDelete={onDelete}
                      typeDefinition={typeDefinition} />,
  });
};

const DecoratorsConfigUpdate = ({ streams, decorators, types }) => {
  const [currentStream, setCurrentStream] = useState(DEFAULT_STREAM_ID);
  const [updatedDecorators, setUpdatedDecorators] = useState(decorators);

  const decoratorsGroupedByStream = groupBy(updatedDecorators, decorator => (decorator.stream || DEFAULT_SEARCH_ID));
  const currentDecorators = decoratorsGroupedByStream[currentStream];
  const sortedDecorators = currentDecorators
    .sort((d1, d2) => d1.order - d2.order);
  const saveConfig = setUpdatedDecorators;
  const decoratorItems = sortedDecorators.map(decorator => _formatDecorator(decorator, updatedDecorators, types, saveConfig));

  const options = [{ label: 'Default Search', value: DEFAULT_SEARCH_ID }, ...streams
    .filter(({ id }) => Object.keys(decoratorsGroupedByStream).includes(id))
    .sort(({ title: key1 }, { title: key2 }) => defaultCompare(key1, key2))
    .map(({ title, id }) => ({ label: title, value: id }))];

  return (
    <React.Fragment>
      <p>Select the stream for which you want to change the set of default decorators.</p>
      <SelectContainer>
        <Select inputId="streams-filter"
                onChange={setCurrentStream}
                options={options}
                clearable={false}
                style={{ width: '100%' }}
                placeholder="There are no decorators configured for any stream."
                value={currentStream} />
      </SelectContainer>

      <p>Use drag and drop to change the execution order of the decorators.</p>

      <DecoratorList decorators={decoratorItems} />
    </React.Fragment>
  );
};

DecoratorsConfigUpdate.propTypes = {};

export default DecoratorsConfigUpdate;
