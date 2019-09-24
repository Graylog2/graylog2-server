Simple page header with only a title and description:
```js
<PageHeader title="This is a simple page">
  <span>Here goes the page description</span>
</PageHeader>
```

Experimental page header with description, support message, and action. Notice
that there is no border around the header, as the `subpage` prop is set:
```js
import { Button } from 'components/graylog';

<PageHeader title="Here goes the page title" lifecycle="experimental" subpage>
  <span>This is a page description</span>
  <span>This is a support message</span>
  <span><Button bsStyle="info">Action</Button></span>
</PageHeader>
```

Page header with only a support link:
```js
<PageHeader title="Another page">
  {null}
  <span><a href="#">Support link</a></span>
</PageHeader>
```
