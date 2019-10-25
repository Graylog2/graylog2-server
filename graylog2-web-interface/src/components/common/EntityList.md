`EntityList` with no items:

```js
<EntityList bsNoItemsStyle="info"
            noItemsText="No items given."
            items={[]} />
```

`EntityList` with two elements:
```js
import { Button, Col, DropdownButton, MenuItem } from 'components/graylog';
import { EntityListItem } from 'components/common';

const entities = [
  {
    id: '1',
    title: 'Dr. Emmett Brown',
    nickname: 'Doc',
    description: '1.21 GIGAWATTS!!!',
  },
  {
    id: '2',
    title: 'Marty McFly',
    nickname: 'Marty',
    description: 'Nobody calls me chicken',
  },
];

const items = entities.map(entity => {
  // You can encapsulate `EntityListItem` into its own component,
  // so it's easy to see what data you transform and feed into it.
  return (
    <EntityListItem key={`entity-${entity.id}`}
                    title={entity.title}
                    titleSuffix={entity.nickname}
                    description={entity.description}
                    actions={[
                      <Button key={1} bsStyle="info">Edit</Button>,
                      <DropdownButton key={2} id="more-dropdown" title="More" pullRight>
                        <MenuItem>Delete</MenuItem>
                      </DropdownButton>,
                    ]}
                    contentRow={
                      <Col md={12}>
                        <p>
                        Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod
                        tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim
                        veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea
                        commodo consequat. Duis aute irure dolor in reprehenderit in voluptate
                        velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat
                        cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id
                        est laborum.
                        </p>
                      </Col>
                    }/>
  )
});

<EntityList bsNoItemsStyle="info"
            noItemsText="No items given."
            items={items} />
```
