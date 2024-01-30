## Setting up keyboard shortcuts

If you want to add a keyboard shortcut, you need to:
- extend the `hotkeysCollections` in the `hotkeysProvider`.
- call `useHotkey` hook in the place which provides the functionality you want to execute on keypress.

## Handling date times

When receiving or sending dates to the backend they are always in UTC and expressed according to ISO 8601, for example `2010-07-30T16:03:25.000Z`.
This is also the preferred format to store date times in the state of, for example, UI components.

When displaying date times in the UI they are always displayed in the user timezone.
The [UserDateTimeProvider](https://github.com/Graylog2/graylog2-server/blob/master/graylog2-web-interface/src/contexts/UserDateTimeProvider.tsx) contains the related functionality
and provides further information. It can be access using the [useUserDateTime](https://github.com/Graylog2/graylog2-server/blob/master/graylog2-web-interface/src/hooks/useUserDateTime.ts) hook.

If you just want to display a date time, you can render the [Timestamp](#timestamp) component, which implements methods provided by the `useUserDateTime` hook.
If you want to display the relative time in a human-readable format you can render the [RelativeTime](#relativetime) component.

For all other cases where you need to transform a date time you can use the [DateTime](https://github.com/Graylog2/graylog2-server/blob/master/graylog2-web-interface/src/util/DateTime.ts) utils.
Instead of using `moment` directly, use or (if necessary) extend the `DateTime` utils. It makes it easier to replace moment with an alternative.
