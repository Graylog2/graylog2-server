Simple example without transforming the given date time:
```jsx
<Timestamp dateTime={new Date()} />
```

Relative time:
```jsx
import moment from 'moment';

const yesterday = moment().subtract(1, 'day');

<span><Timestamp dateTime={yesterday} /> is <Timestamp dateTime={yesterday} relative />.</span>
```

Formatted time:
```jsx
import DateTime from 'logic/datetimes/DateTime';

<Timestamp dateTime={new Date().toISOString()} format={DateTime.Formats.COMPLETE} />
```

Showing date/time of Unix Timestamp (in millis):
```jsx
<Timestamp dateTime={1554121284687} />
```

Time zone conversions:
```jsx
import moment from 'moment';

const nowUtc = moment.utc();

<dl>
  <dt>UTC</dt>
  <dd><Timestamp dateTime={nowUtc}/></dd>
  <dt>Tokyo</dt>
  <dd><Timestamp dateTime={nowUtc} tz="Asia/Tokyo" /></dd>
</dl>
```
