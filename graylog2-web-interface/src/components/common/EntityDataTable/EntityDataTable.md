Define custom cell and header renderer:

```js
import EntityDataTable from './EntityDataTable';

<EntityDataTable
  columnPreferences={{ title: { status: 'show' }, description: { status: 'show' } }}
  data={[
    {
      id: 'row-id',
      title: 'Row title',
      description: 'Row description',
    },
  ]}
  columnDefinitions={[
    { id: 'title', title: 'Title' },
    { id: 'description', title: 'Description' },
  ]}
  columnRenderers={{
    title: {
      renderCell: (listItem) => `The title: ${listItem.title}`,
      renderHeader: (title) => `Custom ${title}`,
    },
  }}
/>;
```

Render row actions:

```js
import EntityDataTable from './EntityDataTable';

<EntityDataTable
  columnPreferences={{ title: { status: 'show' }, description: { status: 'show' } }}
  data={[
    {
      id: 'row-id',
      title: 'Row title',
      description: 'Row description',
    },
  ]}
  columnDefinitions={[
    { id: 'title', title: 'Title' },
    { id: 'description', title: 'Description' },
  ]}
  rowActions={() => (
    <div>
      <button type="button">Actions</button>
    </div>
  )}
/>;
```

Only render a column when the user has the required permissions:

```js
import EntityDataTable from './EntityDataTable';

<EntityDataTable
  columnPreferences={{ title: { status: 'show' }, description: { status: 'show' } }}
  data={[
    {
      id: 'row-id',
      title: 'Row title',
      description: 'Row description',
    },
  ]}
  columnDefinitions={[
    { id: 'title', title: 'Title' },
    { id: 'description', title: 'Description' },
  ]}
  attributePermissions={{
    description: {
      permissions: ['description:read'],
    },
  }}
/>;
```

Change the width of a column, with the related column renderer. All columns have a static width in px.
The actions column fills the remaining space. When the columns need more space than available, the table can be scrolled horizontally.
When a user resizes a column, only this column changes its width.

Column renderers can define a `staticWidth`:

- in px. Please use the `COLUMN_WIDTH` constants for common column content like a title, description or username,
  also when the attribute has a custom name, e.g. a `hostname` column which is the title of the entity.
- or `'matchHeader'`, to use the rendered header width. Useful when the cells contain e.g. just an icon or a short status.

If no `staticWidth` is defined, the column has a width of `DEFAULT_COL_WIDTH`. A width smaller than the header width will be increased to the header width.

Please have a look at the default column renderers defined in the `EntityDataTable`, they already contain widths for common attributes
like `title`, `name` and `description` and for date columns.

```js
import EntityDataTable from './EntityDataTable';
import { COLUMN_WIDTH } from './Constants';

<EntityDataTable
  columnPreferences={{ hostname: { status: 'show' }, summary: { status: 'show' }, status: { status: 'show' } }}
  data={[
    {
      id: 'row-id',
      hostname: 'graylog-node-1',
      summary: 'Entity summary',
      status: 'status',
    },
  ]}
  columnDefinitions={[
    { id: 'hostname', title: 'Hostname' },
    { id: 'summary', title: 'Summary' },
    { id: 'status', title: 'Status' },
  ]}
  columnRenderers={{
    attributes: {
      hostname: {
        staticWidth: COLUMN_WIDTH.title,
      },
      status: {
        staticWidth: 'matchHeader',
      },
    },
  }}
/>;
```
