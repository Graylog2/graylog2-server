Simple page header with only a title and description:
```js
<PageHeader title="This is a simple page">
  <span>Here goes the page description</span>
</PageHeader>
```

Page header with description, documentation link, and action. Notice
that there is no border around the header, as the `subpage` prop is set:
```js
import { Button } from 'components/bootstrap';
import DocsHelper from 'util/DocsHelper';

<PageHeader title="Here goes the page title"
            lifecycle="experimental"
            subpage
            actions={<Button bsStyle="success">Action</Button>}
            documentationLink={{
              title: 'Alerts documentation',
              path: DocsHelper.PAGES.WELCOME,
            }}>
  <span>This is a page description</span>
  <span>This is a support message</span>
</PageHeader>
```