/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.plugins.pipelineprocessor.functions;

import com.google.inject.Binder;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import org.graylog.plugins.pipelineprocessor.ast.functions.Function;
import org.graylog.plugins.pipelineprocessor.functions.conversion.BooleanConversion;
import org.graylog.plugins.pipelineprocessor.functions.conversion.DoubleConversion;
import org.graylog.plugins.pipelineprocessor.functions.conversion.IsBoolean;
import org.graylog.plugins.pipelineprocessor.functions.conversion.IsCollection;
import org.graylog.plugins.pipelineprocessor.functions.conversion.IsDouble;
import org.graylog.plugins.pipelineprocessor.functions.conversion.IsList;
import org.graylog.plugins.pipelineprocessor.functions.conversion.IsLong;
import org.graylog.plugins.pipelineprocessor.functions.conversion.IsMap;
import org.graylog.plugins.pipelineprocessor.functions.conversion.IsNumber;
import org.graylog.plugins.pipelineprocessor.functions.conversion.IsString;
import org.graylog.plugins.pipelineprocessor.functions.conversion.LongConversion;
import org.graylog.plugins.pipelineprocessor.functions.conversion.MapConversion;
import org.graylog.plugins.pipelineprocessor.functions.conversion.StringConversion;
import org.graylog.plugins.pipelineprocessor.functions.dates.DateConversion;
import org.graylog.plugins.pipelineprocessor.functions.dates.FlexParseDate;
import org.graylog.plugins.pipelineprocessor.functions.dates.FormatDate;
import org.graylog.plugins.pipelineprocessor.functions.dates.IsDate;
import org.graylog.plugins.pipelineprocessor.functions.dates.Now;
import org.graylog.plugins.pipelineprocessor.functions.dates.ParseDate;
import org.graylog.plugins.pipelineprocessor.functions.dates.ParseUnixMilliseconds;
import org.graylog.plugins.pipelineprocessor.functions.dates.periods.Days;
import org.graylog.plugins.pipelineprocessor.functions.dates.periods.Hours;
import org.graylog.plugins.pipelineprocessor.functions.dates.periods.IsPeriod;
import org.graylog.plugins.pipelineprocessor.functions.dates.periods.Millis;
import org.graylog.plugins.pipelineprocessor.functions.dates.periods.Minutes;
import org.graylog.plugins.pipelineprocessor.functions.dates.periods.Months;
import org.graylog.plugins.pipelineprocessor.functions.dates.periods.PeriodParseFunction;
import org.graylog.plugins.pipelineprocessor.functions.dates.periods.Seconds;
import org.graylog.plugins.pipelineprocessor.functions.dates.periods.Weeks;
import org.graylog.plugins.pipelineprocessor.functions.dates.periods.Years;
import org.graylog.plugins.pipelineprocessor.functions.debug.Debug;
import org.graylog.plugins.pipelineprocessor.functions.debug.MetricCounterIncrement;
import org.graylog.plugins.pipelineprocessor.functions.encoding.Base16Decode;
import org.graylog.plugins.pipelineprocessor.functions.encoding.Base16Encode;
import org.graylog.plugins.pipelineprocessor.functions.encoding.Base32Decode;
import org.graylog.plugins.pipelineprocessor.functions.encoding.Base32Encode;
import org.graylog.plugins.pipelineprocessor.functions.encoding.Base32HumanDecode;
import org.graylog.plugins.pipelineprocessor.functions.encoding.Base32HumanEncode;
import org.graylog.plugins.pipelineprocessor.functions.encoding.Base64Decode;
import org.graylog.plugins.pipelineprocessor.functions.encoding.Base64Encode;
import org.graylog.plugins.pipelineprocessor.functions.encoding.Base64UrlDecode;
import org.graylog.plugins.pipelineprocessor.functions.encoding.Base64UrlEncode;
import org.graylog.plugins.pipelineprocessor.functions.hashing.CRC32;
import org.graylog.plugins.pipelineprocessor.functions.hashing.CRC32C;
import org.graylog.plugins.pipelineprocessor.functions.hashing.MD5;
import org.graylog.plugins.pipelineprocessor.functions.hashing.Murmur3_128;
import org.graylog.plugins.pipelineprocessor.functions.hashing.Murmur3_32;
import org.graylog.plugins.pipelineprocessor.functions.hashing.SHA1;
import org.graylog.plugins.pipelineprocessor.functions.hashing.SHA256;
import org.graylog.plugins.pipelineprocessor.functions.hashing.SHA512;
import org.graylog.plugins.pipelineprocessor.functions.ips.CidrMatch;
import org.graylog.plugins.pipelineprocessor.functions.ips.IpAddressConversion;
import org.graylog.plugins.pipelineprocessor.functions.ips.IsIp;
import org.graylog.plugins.pipelineprocessor.functions.json.IsJson;
import org.graylog.plugins.pipelineprocessor.functions.json.JsonParse;
import org.graylog.plugins.pipelineprocessor.functions.json.SelectJsonPath;
import org.graylog.plugins.pipelineprocessor.functions.lookup.Lookup;
import org.graylog.plugins.pipelineprocessor.functions.lookup.LookupAddStringList;
import org.graylog.plugins.pipelineprocessor.functions.lookup.LookupClearKey;
import org.graylog.plugins.pipelineprocessor.functions.lookup.LookupRemoveStringList;
import org.graylog.plugins.pipelineprocessor.functions.lookup.LookupSetValue;
import org.graylog.plugins.pipelineprocessor.functions.lookup.LookupStringList;
import org.graylog.plugins.pipelineprocessor.functions.lookup.LookupSetStringList;
import org.graylog.plugins.pipelineprocessor.functions.lookup.LookupValue;
import org.graylog.plugins.pipelineprocessor.functions.messages.CloneMessage;
import org.graylog.plugins.pipelineprocessor.functions.messages.CreateMessage;
import org.graylog.plugins.pipelineprocessor.functions.messages.DropMessage;
import org.graylog.plugins.pipelineprocessor.functions.messages.HasField;
import org.graylog.plugins.pipelineprocessor.functions.messages.RemoveField;
import org.graylog.plugins.pipelineprocessor.functions.messages.RemoveFromStream;
import org.graylog.plugins.pipelineprocessor.functions.messages.RenameField;
import org.graylog.plugins.pipelineprocessor.functions.messages.RouteToStream;
import org.graylog.plugins.pipelineprocessor.functions.messages.SetField;
import org.graylog.plugins.pipelineprocessor.functions.messages.SetFields;
import org.graylog.plugins.pipelineprocessor.functions.messages.StreamCacheService;
import org.graylog.plugins.pipelineprocessor.functions.messages.TrafficAccountingSize;
import org.graylog.plugins.pipelineprocessor.functions.strings.Abbreviate;
import org.graylog.plugins.pipelineprocessor.functions.strings.Capitalize;
import org.graylog.plugins.pipelineprocessor.functions.strings.Concat;
import org.graylog.plugins.pipelineprocessor.functions.strings.Contains;
import org.graylog.plugins.pipelineprocessor.functions.strings.EndsWith;
import org.graylog.plugins.pipelineprocessor.functions.strings.FirstNonNull;
import org.graylog.plugins.pipelineprocessor.functions.strings.GrokMatch;
import org.graylog.plugins.pipelineprocessor.functions.strings.Join;
import org.graylog.plugins.pipelineprocessor.functions.strings.KeyValue;
import org.graylog.plugins.pipelineprocessor.functions.strings.Length;
import org.graylog.plugins.pipelineprocessor.functions.strings.Lowercase;
import org.graylog.plugins.pipelineprocessor.functions.strings.RegexMatch;
import org.graylog.plugins.pipelineprocessor.functions.strings.RegexReplace;
import org.graylog.plugins.pipelineprocessor.functions.strings.Replace;
import org.graylog.plugins.pipelineprocessor.functions.strings.Split;
import org.graylog.plugins.pipelineprocessor.functions.strings.StartsWith;
import org.graylog.plugins.pipelineprocessor.functions.strings.Substring;
import org.graylog.plugins.pipelineprocessor.functions.strings.Swapcase;
import org.graylog.plugins.pipelineprocessor.functions.strings.Uncapitalize;
import org.graylog.plugins.pipelineprocessor.functions.strings.Uppercase;
import org.graylog.plugins.pipelineprocessor.functions.syslog.SyslogFacilityConversion;
import org.graylog.plugins.pipelineprocessor.functions.syslog.SyslogLevelConversion;
import org.graylog.plugins.pipelineprocessor.functions.syslog.SyslogPriorityConversion;
import org.graylog.plugins.pipelineprocessor.functions.syslog.SyslogPriorityToStringConversion;
import org.graylog.plugins.pipelineprocessor.functions.urls.IsUrl;
import org.graylog.plugins.pipelineprocessor.functions.urls.UrlConversion;
import org.graylog.plugins.pipelineprocessor.functions.urls.UrlDecode;
import org.graylog.plugins.pipelineprocessor.functions.urls.UrlEncode;
import org.graylog2.plugin.PluginModule;

public class ProcessorFunctionsModule extends PluginModule {
    @Override
    protected void configure() {
        // built-in functions
        addMessageProcessorFunction(BooleanConversion.NAME, BooleanConversion.class);
        addMessageProcessorFunction(DoubleConversion.NAME, DoubleConversion.class);
        addMessageProcessorFunction(LongConversion.NAME, LongConversion.class);
        addMessageProcessorFunction(StringConversion.NAME, StringConversion.class);
        addMessageProcessorFunction(MapConversion.NAME, MapConversion.class);

        // Comparison functions
        addMessageProcessorFunction(IsBoolean.NAME, IsBoolean.class);
        addMessageProcessorFunction(IsNumber.NAME, IsNumber.class);
        addMessageProcessorFunction(IsDouble.NAME, IsDouble.class);
        addMessageProcessorFunction(IsLong.NAME, IsLong.class);
        addMessageProcessorFunction(IsString.NAME, IsString.class);
        addMessageProcessorFunction(IsCollection.NAME, IsCollection.class);
        addMessageProcessorFunction(IsList.NAME, IsList.class);
        addMessageProcessorFunction(IsMap.NAME, IsMap.class);
        addMessageProcessorFunction(IsDate.NAME, IsDate.class);
        addMessageProcessorFunction(IsPeriod.NAME, IsPeriod.class);
        addMessageProcessorFunction(IsIp.NAME, IsIp.class);
        addMessageProcessorFunction(IsJson.NAME, IsJson.class);
        addMessageProcessorFunction(IsUrl.NAME, IsUrl.class);

        // message related functions
        addMessageProcessorFunction(HasField.NAME, HasField.class);
        addMessageProcessorFunction(SetField.NAME, SetField.class);
        addMessageProcessorFunction(SetFields.NAME, SetFields.class);
        addMessageProcessorFunction(RenameField.NAME, RenameField.class);
        addMessageProcessorFunction(RemoveField.NAME, RemoveField.class);

        addMessageProcessorFunction(DropMessage.NAME, DropMessage.class);
        addMessageProcessorFunction(CreateMessage.NAME, CreateMessage.class);
        addMessageProcessorFunction(CloneMessage.NAME, CloneMessage.class);
        addMessageProcessorFunction(RemoveFromStream.NAME, RemoveFromStream.class);
        addMessageProcessorFunction(RouteToStream.NAME, RouteToStream.class);
        addMessageProcessorFunction(TrafficAccountingSize.NAME, TrafficAccountingSize.class);
        // helper service for route_to_stream
        serviceBinder().addBinding().to(StreamCacheService.class).in(Scopes.SINGLETON);

        // input related functions
        addMessageProcessorFunction(FromInput.NAME, FromInput.class);

        // generic functions
        addMessageProcessorFunction(RegexMatch.NAME, RegexMatch.class);
        addMessageProcessorFunction(RegexReplace.NAME, RegexReplace.class);
        addMessageProcessorFunction(GrokMatch.NAME, GrokMatch.class);
        addMessageProcessorFunction(GrokExists.NAME, GrokExists.class);

        // string functions
        addMessageProcessorFunction(Abbreviate.NAME, Abbreviate.class);
        addMessageProcessorFunction(Capitalize.NAME, Capitalize.class);
        addMessageProcessorFunction(Contains.NAME, Contains.class);
        addMessageProcessorFunction(EndsWith.NAME, EndsWith.class);
        addMessageProcessorFunction(Lowercase.NAME, Lowercase.class);
        addMessageProcessorFunction(Substring.NAME, Substring.class);
        addMessageProcessorFunction(Swapcase.NAME, Swapcase.class);
        addMessageProcessorFunction(Uncapitalize.NAME, Uncapitalize.class);
        addMessageProcessorFunction(Uppercase.NAME, Uppercase.class);
        addMessageProcessorFunction(Concat.NAME, Concat.class);
        addMessageProcessorFunction(KeyValue.NAME, KeyValue.class);
        addMessageProcessorFunction(Join.NAME, Join.class);
        addMessageProcessorFunction(Split.NAME, Split.class);
        addMessageProcessorFunction(StartsWith.NAME, StartsWith.class);
        addMessageProcessorFunction(Replace.NAME, Replace.class);
        addMessageProcessorFunction(Length.NAME, Length.class);
        addMessageProcessorFunction(FirstNonNull.NAME, FirstNonNull.class);

        // json
        addMessageProcessorFunction(JsonParse.NAME, JsonParse.class);
        addMessageProcessorFunction(SelectJsonPath.NAME, SelectJsonPath.class);

        // dates
        addMessageProcessorFunction(DateConversion.NAME, DateConversion.class);
        addMessageProcessorFunction(Now.NAME, Now.class);
        addMessageProcessorFunction(ParseDate.NAME, ParseDate.class);
        addMessageProcessorFunction(ParseUnixMilliseconds.NAME, ParseUnixMilliseconds.class);
        addMessageProcessorFunction(FlexParseDate.NAME, FlexParseDate.class);
        addMessageProcessorFunction(FormatDate.NAME, FormatDate.class);
        addMessageProcessorFunction(Years.NAME, Years.class);
        addMessageProcessorFunction(Months.NAME, Months.class);
        addMessageProcessorFunction(Weeks.NAME, Weeks.class);
        addMessageProcessorFunction(Days.NAME, Days.class);
        addMessageProcessorFunction(Hours.NAME, Hours.class);
        addMessageProcessorFunction(Minutes.NAME, Minutes.class);
        addMessageProcessorFunction(Seconds.NAME, Seconds.class);
        addMessageProcessorFunction(Millis.NAME, Millis.class);
        addMessageProcessorFunction(PeriodParseFunction.NAME, PeriodParseFunction.class);

        // hash digest
        addMessageProcessorFunction(CRC32.NAME, CRC32.class);
        addMessageProcessorFunction(CRC32C.NAME, CRC32C.class);
        addMessageProcessorFunction(MD5.NAME, MD5.class);
        addMessageProcessorFunction(Murmur3_32.NAME, Murmur3_32.class);
        addMessageProcessorFunction(Murmur3_128.NAME, Murmur3_128.class);
        addMessageProcessorFunction(SHA1.NAME, SHA1.class);
        addMessageProcessorFunction(SHA256.NAME, SHA256.class);
        addMessageProcessorFunction(SHA512.NAME, SHA512.class);

        // encoding
        addMessageProcessorFunction(Base16Encode.NAME, Base16Encode.class);
        addMessageProcessorFunction(Base16Decode.NAME, Base16Decode.class);
        addMessageProcessorFunction(Base32Encode.NAME, Base32Encode.class);
        addMessageProcessorFunction(Base32Decode.NAME, Base32Decode.class);
        addMessageProcessorFunction(Base32HumanEncode.NAME, Base32HumanEncode.class);
        addMessageProcessorFunction(Base32HumanDecode.NAME, Base32HumanDecode.class);
        addMessageProcessorFunction(Base64Encode.NAME, Base64Encode.class);
        addMessageProcessorFunction(Base64Decode.NAME, Base64Decode.class);
        addMessageProcessorFunction(Base64UrlEncode.NAME, Base64UrlEncode.class);
        addMessageProcessorFunction(Base64UrlDecode.NAME, Base64UrlDecode.class);

        // ip handling
        addMessageProcessorFunction(CidrMatch.NAME, CidrMatch.class);
        addMessageProcessorFunction(IpAddressConversion.NAME, IpAddressConversion.class);

        // null support
        addMessageProcessorFunction(IsNull.NAME, IsNull.class);
        addMessageProcessorFunction(IsNotNull.NAME, IsNotNull.class);

        // URL parsing
        addMessageProcessorFunction(UrlConversion.NAME, UrlConversion.class);
        addMessageProcessorFunction(UrlDecode.NAME, UrlDecode.class);
        addMessageProcessorFunction(UrlEncode.NAME, UrlEncode.class);

        // Syslog support
        addMessageProcessorFunction(SyslogFacilityConversion.NAME, SyslogFacilityConversion.class);
        addMessageProcessorFunction(SyslogLevelConversion.NAME, SyslogLevelConversion.class);
        addMessageProcessorFunction(SyslogPriorityConversion.NAME, SyslogPriorityConversion.class);
        addMessageProcessorFunction(SyslogPriorityToStringConversion.NAME, SyslogPriorityToStringConversion.class);

        // Lookup tables
        addMessageProcessorFunction(Lookup.NAME, Lookup.class);
        addMessageProcessorFunction(LookupValue.NAME, LookupValue.class);
        addMessageProcessorFunction(LookupStringList.NAME, LookupStringList.class);
        addMessageProcessorFunction(LookupSetValue.NAME, LookupSetValue.class);
        addMessageProcessorFunction(LookupClearKey.NAME, LookupClearKey.class);
        addMessageProcessorFunction(LookupSetStringList.NAME, LookupSetStringList.class);
        addMessageProcessorFunction(LookupAddStringList.NAME, LookupAddStringList.class);
        addMessageProcessorFunction(LookupRemoveStringList.NAME, LookupRemoveStringList.class);

        // Debug
        addMessageProcessorFunction(Debug.NAME, Debug.class);
        addMessageProcessorFunction(MetricCounterIncrement.NAME, MetricCounterIncrement.class);
    }

    protected void addMessageProcessorFunction(String name, Class<? extends Function<?>> functionClass) {
        addMessageProcessorFunction(binder(), name, functionClass);
    }

    public static MapBinder<String, Function<?>> processorFunctionBinder(Binder binder) {
        return MapBinder.newMapBinder(binder, TypeLiteral.get(String.class), new TypeLiteral<Function<?>>() {});
    }

    public static void addMessageProcessorFunction(Binder binder, String name, Class<? extends Function<?>> functionClass) {
        processorFunctionBinder(binder).addBinding(name).to(functionClass);

    }
}
