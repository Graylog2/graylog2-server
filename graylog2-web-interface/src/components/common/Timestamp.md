For the following examples we are using the date time `2010-07-30T16:03:25.000Z`.
By default, the output will be based on the user time zone defined for the style guide.

#### Default

The component displays the date time in the default format for date times in the graylog UI, when no format is specified.

```tsx
<Timestamp dateTime="2010-07-30T16:03:25.000Z" />
```

#### Specific timezone

In this example we are displaying the provided time as UTC. 

```tsx
<Timestamp dateTime="2010-07-30T16:03:25.000Z" tz="UTC" />
```

#### Different formats

```tsx
import { DATE_TIME_FORMATS } from 'util/DateTime';


<table cellPadding="10">
  <thead>
    <tr>
      <th style={{ width: '150px' }}>Format</th>
      <th>Output</th>
    </tr>
  </thead>
  <tbody>
    {Object.keys(DATE_TIME_FORMATS).map((format) => (
      <tr key={format}>
        <td>{format}</td>
        <td><Timestamp dateTime="2010-07-30T16:03:25.000Z" format={format}/></td>
      </tr>
    ))}
  </tbody>
</table>
```
