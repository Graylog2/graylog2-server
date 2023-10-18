```tsx

import useUserDateTime from 'hooks/useUserDateTime';

const UsageExample = () => {
  const { userTimezone, formatTime, toUserTimezone } = useUserDateTime();
  
  return (
    <>
      The hook provides:<br />
      <ul>
        <li><i>userTimezone</i> value, which is the time zone of the current user: {userTimezone}.</li>
        <li>
          <i>formatTime</i> method, which takes a date like `2010-07-30T16:03:25.000Z` and transforms it to the user timezone: {formatTime('2010-07-30T16:03:25.000Z')}.
          It also supports different formats.
        </li>
        <li><i>toUserTimezone</i> method, which takes a date like `2010-07-30T16:03:25.000Z` and creates a moment date object with the user timezone: {toUserTimezone('2010-07-30T16:03:25.000Z').toString()} (Here used in combination with .toString() for demonstration purpose).</li>
      </ul>
    </>
  )
}

<UsageExample />
```
