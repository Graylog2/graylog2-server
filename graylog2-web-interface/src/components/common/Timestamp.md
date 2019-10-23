Simple example without transforming the given date time:
```js
<Timestamp dateTime={new Date()} />
```

Relative time:
```js
import moment from 'moment';
const yesterday = moment().subtract(1, 'day');
<span><Timestamp dateTime={yesterday} /> is <Timestamp dateTime={yesterday} relative />.</span>
```

Formatted time:
```js
const DateTime = require('logic/datetimes/DateTime');
<Timestamp dateTime={new Date().toISOString()} format={DateTime.Formats.COMPLETE} />
```

Showing date/time of Unix Timestamp (in millis):
```js
<Timestamp datetime={1554121284687} />
```

Time zone conversions:
```js
import moment from 'moment';
const nowUtc = moment.utc();
<dl>
  <dt>UTC</dt>
  <dd><Timestamp dateTime={nowUtc}/></dd>
  <dt>Tokyo</dt>
  <dd><Timestamp dateTime={nowUtc} tz="Asia/Tokyo" /></dd>
</dl>
```
