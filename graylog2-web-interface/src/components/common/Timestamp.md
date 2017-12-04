Simple example without transforming the given date time:
```js
<Timestamp dateTime={new Date()} />
```

Relative time:
```js
const moment = require('moment');
const yesterday = moment().subtract(1, 'day');
<span><Timestamp dateTime={yesterday} /> is <Timestamp dateTime={yesterday} relative />.</span>
```

Formatted time:
```js
const DateTime = require('logic/datetimes/DateTime');
<Timestamp dateTime={new Date().toISOString()} format={DateTime.Formats.COMPLETE} />
```

Time zone conversions:
```js
const moment = require('moment');
const nowUtc = moment.utc();
<dl>
  <dt>UTC</dt>
  <dd><Timestamp dateTime={nowUtc}/></dd>
  <dt>Tokyo</dt>
  <dd><Timestamp dateTime={nowUtc} tz="Asia/Tokyo" /></dd>
</dl>
```
