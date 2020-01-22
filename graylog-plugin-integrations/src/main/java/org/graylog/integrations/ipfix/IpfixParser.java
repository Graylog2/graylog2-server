/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.integrations.ipfix;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.primitives.Longs;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * A Graylog specific IPFIX parser.
 * <p>
 * This IPFIX parser supports two modes:
 * <ol>
 * <li>Parse as little of a packet as possible to be used in the input, just enough to make sure that we have all
 * template sets for the data sets we've received.</li>
 * <li>Completely parse the content of a packet, requiring all template sets to be known as well as all information
 * elements to be declared (possibly via configuration files).
 * </li>
 * </ol>
 * </p>
 */
public class IpfixParser {
    private static final Logger LOG = LoggerFactory.getLogger(IpfixParser.class);
    private static final int SETID_RESERVED0 = 0;
    private static final int SETID_RESERVED1 = 1;
    private static final int SETID_TEMPLATE = 2;
    private static final int SETID_OPTIONSTEMPLATE = 3;

    private final InformationElementDefinitions infoElemDefs;

    public IpfixParser(InformationElementDefinitions informationElementDefinitions) {
        this.infoElemDefs = informationElementDefinitions;
    }

    /**
     * Parse an IPFIX message out of the given packet buffer.
     * <p>
     * Decodes enough of the given packet to be able to tell whether we have all necessary information to parse the contained
     * sets completely. This typically means to see whether any unknown template ids are referenced, in which case we need
     * to hang on to the data records until we have received the missing templates.
     * </p>
     *
     * @param packet buffer containing the received packet bytes
     * @return the packet description
     */
    public MessageDescription shallowParseMessage(ByteBuf packet) {
        final ByteBuf buffer = packet.readSlice(MessageHeader.LENGTH);
        LOG.debug("Shallow parse header\n{}", ByteBufUtil.prettyHexDump(buffer));
        final MessageHeader header = parseMessageHeader(buffer);
        final MessageDescription messageDescription = new MessageDescription(header);

        // sanity check: we need the complete packet in the buffer
        if (header.length() != packet.readableBytes() + MessageHeader.LENGTH) {
            throw new IllegalArgumentException("Buffer does not contain the complete IPFIX message");
        }
        // loop over all the contained sets in the message
        while (packet.isReadable()) {
            final int setId = packet.readUnsignedShort();
            final int setLength = packet.readUnsignedShort();
            // the buffer limited to the declared length of the set.
            final ByteBuf setContent = packet.readSlice(setLength - 4);
            switch (setId) {
                case 0:
                case 1:
                    throw new IpfixException("Invalid set id in IPFIX message: " + setId);
                case 2:
                    final ShallowTemplateSet templateSet = shallowParseTemplateSet(setContent);
                    messageDescription.addTemplateSet(templateSet);
                    break;
                case 3:
                    final ShallowOptionsTemplateSet optionsTemplateSet = shallowParseOptionsTemplateSet(setContent);
                    messageDescription.addOptionsTemplateSet(optionsTemplateSet);
                    break;
                default:
                    final ShallowDataSet dataSet = shallowParseDataSet(setId, setLength, setContent, header.exportTime());
                    messageDescription.addDataSet(dataSet);
                    break;
            }
        }
        return messageDescription;
    }

    @SuppressWarnings("Duplicates")
    private ShallowTemplateSet shallowParseTemplateSet(ByteBuf setContent) {
        // a template set consists of multiple template records, the first 2 bytes of each records tell us the template id
        // which is what we need to for shallow parsing. the rest of each record we save for parsing it later, even though
        // we actually have to parse all the information elements right away, because there's no length field for a
        // template record so we cannot skip ahead, go figure :/

        LOG.debug("Attempting a shallow parse on template set.");
        final ImmutableList.Builder<ShallowTemplateSet.Record> builder = ImmutableList.builder();
        while (setContent.isReadable()) {
            // remember current read index so we can make a copy of the entire template record after parsing it
            setContent.markReaderIndex();
            final int lowerReaderIndex = setContent.readerIndex();
            final int templateId = setContent.readUnsignedShort();
            final int fieldCount = setContent.readUnsignedShort();
            for (int i = 0; i < fieldCount; i++) {
                // discard, we don't actually need it right now, but we need to parse them because we cannot know the
                // record length from just the header :/
                parseInformationElement(setContent);
            }
            // copy entire template record for later
            final int upperReaderIndex = setContent.readerIndex();
            final byte[] recordBytes = new byte[upperReaderIndex - lowerReaderIndex];
            setContent.resetReaderIndex();
            setContent.readBytes(recordBytes);

            builder.add(new ShallowTemplateSet.Record(templateId, recordBytes));
        }
        return ShallowTemplateSet.create(builder.build());
    }

    @SuppressWarnings("Duplicates")
    private ShallowOptionsTemplateSet shallowParseOptionsTemplateSet(ByteBuf setContent) {
        // an options template set consists of multiple options template records, the first 2 bytes of each records tell
        // us the template id which is what we need to for shallow parsing. the rest of each record we save for parsing
        // it later, even though  we actually have to parse all the information elements right away, because there's no
        // length field for an options template record so we cannot skip ahead, go figure :/
        LOG.debug("Attempting a shallow parse on options template set.");
        final ImmutableList.Builder<ShallowOptionsTemplateSet.Record> builder = ImmutableList.builder();
        while (setContent.isReadable()) {
            // remember current read index so we can make a copy of the entire template record after parsing it
            setContent.markReaderIndex();
            final int lowerReaderIndex = setContent.readerIndex();
            final int templateId = setContent.readUnsignedShort();
            final int fieldCount = setContent.readUnsignedShort();
            final int scopeFieldCount = setContent.readUnsignedShort();
            for (int i = 0; i < fieldCount; i++) {
                // discard, we don't actually need it right now, but we need to parse them because we cannot know the
                // record length from just the header :/
                parseInformationElement(setContent);
            }
            // copy entire template record for later
            final int upperReaderIndex = setContent.readerIndex();
            final byte[] recordBytes = new byte[upperReaderIndex - lowerReaderIndex];
            setContent.resetReaderIndex();
            setContent.readBytes(recordBytes);

            builder.add(new ShallowOptionsTemplateSet.Record(templateId, recordBytes));
        }
        return ShallowOptionsTemplateSet.create(builder.build());
    }

    private ShallowDataSet shallowParseDataSet(int id, int length, ByteBuf setContent, ZonedDateTime exportTime) {
        // the entire data set content minus the template id and length field
        // contains all data records, for which we need the corresponding template records to parse them
        LOG.debug("Attempting a shallow parse on dataset.");
        final byte[] setBytes = new byte[length - 4];
        setContent.readBytes(setBytes);
        return ShallowDataSet.create(id, exportTime.toEpochSecond(), setBytes);
    }

    private InformationElement parseInformationElement(ByteBuf buffer) {
        final int idAndEnterpriseBit = buffer.readUnsignedShort();
        int id = idAndEnterpriseBit;
        long enterpriseNumber = 0;
        final int length = buffer.readUnsignedShort();
        if (idAndEnterpriseBit > 0x8000) {
            id -= 0x8000;
            enterpriseNumber = buffer.readUnsignedInt();
        }
        return InformationElement.create(id, length, enterpriseNumber);
    }

    private MessageHeader parseMessageHeader(ByteBuf buffer) {
        LOG.debug("Attempting to parse message header.");
        final int versionNumber = buffer.readUnsignedShort();
        if (versionNumber != 10) {
            throw new InvalidMessageVersion(versionNumber);
        }
        final int packetLength = buffer.readUnsignedShort();
        final long exportTime = buffer.readUnsignedInt();
        final long sequenceNumber = buffer.readUnsignedInt();
        final long observationDomainId = buffer.readUnsignedInt();
        return MessageHeader.create(packetLength,
                                    ZonedDateTime.ofInstant(Instant.ofEpochSecond(exportTime), ZoneOffset.UTC),
                                    sequenceNumber,
                                    observationDomainId);
    }

    /**
     * Parse the given packet buffer into an IPFIX message.
     * <p>
     * This method requires that all templates are contained in the given packet buffer, contrary to what RFC 7011 requires.
     * Specifically, RFC 7011 Sec 8 "Template Management" says:
     * <pre>
     * However, a Collecting Process MUST NOT assume that the Data Set and the associated Template Set (or Options
     * Template Set) are exported in the same IPFIX Message.
     * </pre>
     * For the purposes of Graylog's input mechanism, we need to ensure that each journal entry can be decoded on its own
     * which either requires some out of band communication of (options) templates, or storing the relevant templates
     * with the datasets in each journal entry.
     * </p>
     *
     * @param packet
     * @return
     */
    public IpfixMessage parseMessage(ByteBuf packet) {
        LOG.debug("Attempting to parse message.");
        LOG.debug("IPFIX message\n{}", ByteBufUtil.prettyHexDump(packet));
        final IpfixMessage.Builder builder = IpfixMessage.builder();
        final ByteBuf headerBuffer = packet.readSlice(MessageHeader.LENGTH);
        LOG.debug("Message header buffer\n{}", ByteBufUtil.prettyHexDump(headerBuffer));
        final MessageHeader header = parseMessageHeader(headerBuffer);
        // sanity check: we need the complete packet in the buffer
        if (header.length() > packet.readableBytes() + MessageHeader.LENGTH) {
            LOG.error("Buffer does not contain expected IPFIX message:\n{}", ByteBufUtil.prettyHexDump(packet));
            throw new IpfixException("Buffer does not contain the complete IPFIX message");
        }
        // loop over all the contained sets in the message
        final Map<Integer, TemplateRecord> templates = Maps.newHashMap();
        final Map<Integer, OptionsTemplateRecord> optionsTemplates = Maps.newHashMap();
        while (packet.isReadable()) {
            final int setId = packet.readUnsignedShort();
            final int setLength = packet.readUnsignedShort();
            LOG.debug("Set id {} buffer\n{}", setId, ByteBufUtil.prettyHexDump(packet, packet.readerIndex() - 4, setLength));
            // the buffer limited to the declared length of the set.
            final ByteBuf setContent = packet.readSlice(setLength - 4);
            switch (setId) {
                case SETID_RESERVED0:
                case SETID_RESERVED1:
                    throw new IpfixException("Invalid set id in IPFIX message: " + setId);
                case SETID_TEMPLATE:
                    final Set<TemplateRecord> templateRecords = parseTemplateSet(setContent);
                    builder.addAllTemplates(templateRecords);
                    templateRecords.forEach(r -> templates.put(r.templateId(), r));
                    break;
                case SETID_OPTIONSTEMPLATE:
                    final Set<OptionsTemplateRecord> optionsTemplateRecords = parseOptionsTemplateSet(setContent);
                    builder.addAllOptionsTemplateSet(optionsTemplateRecords);
                    optionsTemplateRecords.forEach(r -> optionsTemplates.put(r.templateId(), r));
                    break;
                default:
                    ImmutableList<InformationElement> informationElements;
                    if (templates.containsKey(setId)) {
                        informationElements = templates.get(setId).informationElements();
                    } else if (optionsTemplates.containsKey(setId)) {
                        final OptionsTemplateRecord record = optionsTemplates.get(setId);
                        informationElements = ImmutableList.<InformationElement>builder()
                                .addAll(record.scopeFields())
                                .addAll(record.optionFields())
                                .build();
                    } else {
                        throw new IpfixException("Missing template for data set using template id " + setId + ". Cannot parse data set.");
                    }
                    final Set<Flow> flows = parseDataSet(informationElements, templates, setContent);
                    builder.addAllFlows(flows);
                    break;
            }
        }
        return builder.build();

    }

    private Set<TemplateRecord> parseTemplateSet(ByteBuf setContent) {
        LOG.debug("Attempting to parse template set.");
        final ImmutableSet.Builder<TemplateRecord> b = ImmutableSet.builder();
        // TODO consider record padding
        while (setContent.isReadable()) {
            b.add(parseTemplateRecord(setContent));
        }
        return b.build();
    }

    private Set<OptionsTemplateRecord> parseOptionsTemplateSet(ByteBuf setContent) {
        final ImmutableSet.Builder<OptionsTemplateRecord> b = ImmutableSet.builder();
        // TODO consider record padding
        while (setContent.isReadable()) {
            final OptionsTemplateRecord.Builder recordBuilder = OptionsTemplateRecord.builder();
            final int templateId = setContent.readUnsignedShort();
            recordBuilder.templateId(templateId);
            final int fieldCount = setContent.readUnsignedShort();
            final int scopeFieldCount = setContent.readUnsignedShort();
            // there is at least one scope field
            for (int i = 0; i < scopeFieldCount; i++) {
                recordBuilder.scopeFieldsBuilder().add(parseInformationElement(setContent));
            }
            // the remaining fields are option fields
            for (int i = 0; i < fieldCount - scopeFieldCount; i++) {
                recordBuilder.optionFieldsBuilder().add(parseInformationElement(setContent));
            }
            b.add(recordBuilder.build());
        }
        return b.build();
    }

    /**
     * Parses a data set into its individual flows, based on the informationElements from the template ID the data set specified.
     * <p>
     * In order to be able to parse subtemplateList and subtemplateMultilist information elements, the entire templateMap is also passed in.
     * Unfortunately it is not possible to determine which templates lists refer to without actually parsing the data first.
     *
     * @param informationElements the field information from the template used by this data set
     * @param templateMap map from template id to its information elements, used for subtemplateLists
     * @param setContent the data set bytes to parse
     * @return collection of parsed flows
     */
    public Set<Flow> parseDataSet(ImmutableList<InformationElement> informationElements, Map<Integer, TemplateRecord> templateMap, ByteBuf setContent) {
        ImmutableSet.Builder<Flow> flowBuilder = ImmutableSet.builder();
        while (setContent.isReadable()) {
            final ImmutableMap.Builder<String, Object> fields = ImmutableMap.builder();
            for (InformationElement informationElement : informationElements) {
                InformationElementDefinition desc = infoElemDefs.getDefinition(informationElement.id(), informationElement.enterpriseNumber());
                switch (desc.dataType()) {
                    // these are special because they can use reduced-size encoding (RFC 7011 Sec 6.2)
                    case UNSIGNED8:
                    case UNSIGNED16:
                    case UNSIGNED32:
                    case UNSIGNED64:
                        long unsignedValue;
                        switch (informationElement.length()) {
                            case 1:
                                unsignedValue = setContent.readUnsignedByte();
                                break;
                            case 2:
                                unsignedValue = setContent.readUnsignedShort();
                                break;
                            case 3:
                                unsignedValue = setContent.readUnsignedMedium();
                                break;
                            case 4:
                                unsignedValue = setContent.readUnsignedInt();
                                break;
                            case 5:
                            case 6:
                            case 7:
                            case 8:
                                byte[] bytesBigEndian = {0, 0, 0, 0, 0, 0, 0, 0};
                                int firstIndex = 8 - informationElement.length();
                                setContent.readBytes(bytesBigEndian, firstIndex, informationElement.length());
                                unsignedValue = Longs.fromByteArray(bytesBigEndian);
                                break;
                            default:
                                throw new IpfixException("Unexpected length for unsigned integer");
                        }
                        fields.put(desc.fieldName(), unsignedValue);
                        break;
                    case SIGNED8:
                    case SIGNED16:
                    case SIGNED32:
                    case SIGNED64:
                        long signedValue;
                        switch (informationElement.length()) {
                            case 1:
                                signedValue = setContent.readByte();
                                break;
                            case 2:
                                signedValue = setContent.readShort();
                                break;
                            case 3:
                                signedValue = setContent.readMedium();
                                break;
                            case 4:
                                signedValue = setContent.readUnsignedInt();
                                break;
                            case 5:
                            case 6:
                            case 7:
                            case 8:
                                byte[] bytesBigEndian = {0, 0, 0, 0, 0, 0, 0, 0};
                                int firstIndex = 8 - informationElement.length() - 1;
                                setContent.readBytes(bytesBigEndian, firstIndex, informationElement.length());
                                signedValue = Longs.fromByteArray(bytesBigEndian);
                                break;
                            default:
                                throw new IpfixException("Unexpected length for unsigned integer");
                        }
                        fields.put(desc.fieldName(), signedValue);
                        break;
                    case FLOAT32:
                    case FLOAT64:
                        double floatValue;
                        switch (informationElement.length()) {
                            case 4:
                                floatValue = setContent.readFloat();
                                break;
                            case 8:
                                floatValue = setContent.readDouble();
                                break;
                            default:
                                throw new IpfixException("Unexpected length for float value: " + informationElement.length());
                        }
                        fields.put(desc.fieldName(), floatValue);
                        break;
                    // the remaining types aren't subject to reduced-size encoding
                    case MACADDRESS:
                        byte[] macBytes = new byte[6];
                        setContent.readBytes(macBytes);
                        fields.put(desc.fieldName(),
                                   String.format(Locale.ROOT, "%02x:%02x:%02x:%02x:%02x:%02x",
                                                 macBytes[0], macBytes[1], macBytes[2], macBytes[3], macBytes[4], macBytes[5]));

                        break;
                    case IPV4ADDRESS:
                        byte[] ipv4Bytes = new byte[4];
                        setContent.readBytes(ipv4Bytes);
                        try {
                            fields.put(desc.fieldName(), InetAddress.getByAddress(ipv4Bytes).getHostAddress());
                        } catch (UnknownHostException e) {
                            throw new IpfixException("Unable to parse IPV4 address", e);
                        }
                        break;
                    case IPV6ADDRESS:
                        byte[] ipv6Bytes = new byte[16];
                        setContent.readBytes(ipv6Bytes);
                        try {
                            fields.put(desc.fieldName(), InetAddress.getByAddress(ipv6Bytes).getHostAddress());
                        } catch (UnknownHostException e) {
                            throw new IpfixException("Unable to parse IPV6 address", e);
                        }
                        break;
                    case BOOLEAN:
                        final byte booleanByte = setContent.readByte();
                        switch (booleanByte) {
                            case 1:
                                fields.put(desc.fieldName(), true);
                                break;
                            case 2:
                                fields.put(desc.fieldName(), false);
                                break;
                            default:
                                throw new IpfixException("Invalid value for boolean: " + booleanByte);
                        }
                        break;
                    case STRING:
                        final CharSequence charSequence;
                        if (informationElement.length() == 65535) {
                            // variable length element, parse accordingly
                            int length = getVarLength(setContent);
                            charSequence = setContent.readCharSequence(length, StandardCharsets.UTF_8);
                        } else {
                            // fixed length element, just read the string from the buffer
                            charSequence = setContent.readCharSequence(informationElement.length(), StandardCharsets.UTF_8);
                        }

                        fields.put(desc.fieldName(), String.valueOf(charSequence).replace("\0", ""));
                        break;
                    case OCTETARRAY:
                        final byte[] octetArray;
                        if (informationElement.length() == 65535) {
                            int length = getVarLength(setContent);
                            octetArray = new byte[length];
                        } else {
                            octetArray = new byte[informationElement.length()];
                        }
                        setContent.readBytes(octetArray);
                        fields.put(desc.fieldName(), Hex.encodeHexString(octetArray));
                        break;
                    case DATETIMESECONDS:
                        final long dateTimeSeconds = setContent.readUnsignedInt();
                        fields.put(desc.fieldName(), ZonedDateTime.ofInstant(Instant.ofEpochSecond(dateTimeSeconds), ZoneOffset.UTC));
                        break;
                    case DATETIMEMILLISECONDS:
                        final long dateTimeMills = setContent.readLong();
                        fields.put(desc.fieldName(), ZonedDateTime.ofInstant(Instant.ofEpochMilli(dateTimeMills), ZoneOffset.UTC));
                        break;
                    case DATETIMEMICROSECONDS:
                    case DATETIMENANOSECONDS:
                        final long seconds = setContent.readUnsignedInt();
                        long fraction = setContent.readUnsignedInt();
                        if (desc.dataType() == InformationElementDefinition.DataType.DATETIMEMICROSECONDS) {
                            // bottom 11 bits must be cleared for micros to ensure the precision is correct (RFC 7011 Sec 6.1.9)
                            fraction = fraction & ~0x7FF;
                        }
                        fields.put(desc.fieldName(), ZonedDateTime.ofInstant(Instant.ofEpochSecond(seconds, fraction), ZoneOffset.UTC));
                        break;
                    case BASICLIST: {
                        // TODO add to field somehow
                        int length = informationElement.length() == 65535 ? getVarLength(setContent) : setContent.readUnsignedByte();
                        ByteBuf listBuffer = setContent.readSlice(length);
                        final short semantic = listBuffer.readUnsignedByte();
                        final InformationElement element = parseInformationElement(listBuffer);
                        InformationElementDefinition def = infoElemDefs.getDefinition(element.id(), element.enterpriseNumber());
                        if (def == null) {
                            LOG.error("Unable to find information element definition in basicList: id {} PEN {}, this is a bug, cannot parse packet.", element.id(), element.enterpriseNumber());
                            break;
                        } else {
                            LOG.warn("Skipping basicList data ({} bytes)", informationElement.length());
                            while (listBuffer.isReadable()) {
                                // simply discard the bytes for now
                                listBuffer.skipBytes(element.length());
                            }
                        }
                        break;
                    }
                    case SUBTEMPLATELIST: {
                        // there are three possibilities here (compare https://tools.ietf.org/html/rfc6313#section-4.5.2):
                        //  1. the data set's template has an explicit length
                        //  2. the length is < 255 encoded as 1 byte, in variable length format (not recommended)
                        //  3. the length is encoded as 3 bytes, in variable length format (recommended per RFC 6313)
                        /* encoding format in this case is according to Figure 5:
                            0                   1                   2                   3
                            0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
                           +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
                           |   Semantic    |         Template ID           |     ...       |
                           +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
                           |                subTemplateList Content    ...                 |
                           +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
                           |                              ...                              |
                           +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
                          Semantic is one of:
                            * 0xFF - undefined
                            * 0x00 - noneOf
                            * 0x01 - exactlyOneOf
                            * 0x02 - oneOrMoreOf
                            * 0x03 - allOf
                            * 0x04 - ordered
                         */
                        int length = informationElement.length() == 65535 ? getVarLength(setContent) : setContent.readUnsignedByte();
                        // adjust length for semantic + templateId
                        length -= 3;
                        LOG.debug("Remaining data buffer:\n{}", ByteBufUtil.prettyHexDump(setContent));
                        // TODO add to field somehow
                        final short semantic = setContent.readUnsignedByte();
                        final int templateId = setContent.readUnsignedShort();
                        final TemplateRecord templateRecord = templateMap.get(templateId);
                        if (templateRecord == null) {
                            LOG.error("Unable to parse subtemplateList, because we don't have the template for it: {}, skipping data ({} bytes)", templateId, length);
                            setContent.skipBytes(length);
                            break;
                        }
                        final ByteBuf listContent = setContent.readSlice(length);
                        // if this is not readable, it's an empty list
                        final ImmutableList.Builder<Flow> flowsBuilder = ImmutableList.builder();
                        if (listContent.isReadable()) {
                            flowsBuilder.addAll(parseDataSet(templateRecord.informationElements(), templateMap, listContent));
                        }
                        final ImmutableList<Flow> flows = flowsBuilder.build();
                        // flatten arrays and fields into the field name until we have support for nested objects
                        for (int i = 0; i < flows.size(); i++) {
                            final String fieldPrefix = desc.fieldName() + "_" + i + "_";
                            flows.get(i).fields().forEach((field, value) -> {
                                fields.put(fieldPrefix + field, value);
                            });
                        }
                        break;
                    }
                    case SUBTEMPLATEMULTILIST: {
                        int length = informationElement.length() == 65535 ? getVarLength(setContent) : setContent.readUnsignedByte();
                        setContent.skipBytes(length);
                        LOG.warn("subtemplateMultilist support is not implemented, skipping data ({} bytes)", length);
                        break;
                    }
                }
            }
            flowBuilder.add(Flow.create(fields.build()));
        }
        return flowBuilder.build();
    }

    private int getVarLength(ByteBuf setContent) {
        int length;
        final short firstLengthByte = setContent.readUnsignedByte();
        if (firstLengthByte == 255) {
            // > 255 bytes in length, parse two more bytes for actual length
            length = setContent.readUnsignedShort();
        } else {
            length = firstLengthByte;
        }
        return length;
    }

    public TemplateRecord parseTemplateRecord(ByteBuf bytes) {
        final TemplateRecord.Builder recordBuilder = TemplateRecord.builder();
        final int templateId = bytes.readUnsignedShort();
        recordBuilder.templateId(templateId);
        final int fieldCount = bytes.readUnsignedShort();
        for (int i = 0; i < fieldCount; i++) {
            recordBuilder.addInformationElement(parseInformationElement(bytes));
        }
        return recordBuilder.build();
    }

    /**
     * High level description of a shallowly parsed packet.
     * <p>
     * Contains {@link ByteBuf}s of each set from the packet and parses enough metadata about them to allow the caller
     * to make a decision whether we can completely parse the IPFIX message without waiting for template sets.
     * </p>
     */
    public class MessageDescription {

        // we don't care about the sets, the records actually contain the templates we need
        private final Map<Integer, ShallowTemplateSet.Record> templates = Maps.newHashMap();

        // we don't care about the sets, the records actually contain the templates we need
        private final Map<Integer, ShallowOptionsTemplateSet.Record> optionsTemplates = Maps.newHashMap();

        // here we do need the set, because we cannot parse the records without having the template information for them
        // all records in a Data Set use the same template, in fact a record is simply the concatenation of the field values
        private final Multimap<Integer, ShallowDataSet> dataSets = ArrayListMultimap.create();
        private final MessageHeader header;

        public MessageDescription(MessageHeader header) {
            this.header = header;
        }

        public MessageHeader getHeader() {
            return header;
        }

        public void addTemplateSet(ShallowTemplateSet templateSet) {
            for (ShallowTemplateSet.Record record : templateSet.records()) {
                templates.put(record.getTemplateId(), record);
            }
        }

        public ShallowTemplateSet.Record getTemplateRecord(int templateId) {
            return templates.get(templateId);
        }

        public void addOptionsTemplateSet(ShallowOptionsTemplateSet optionsTemplateSet) {
            for (ShallowOptionsTemplateSet.Record record : optionsTemplateSet.records()) {
                optionsTemplates.put(record.getTemplateId(), record);
            }
        }

        public void addDataSet(ShallowDataSet dataSet) {
            dataSets.put(dataSet.templateId(), dataSet);
        }

        public Set<Integer> referencedTemplateIds() {
            return ImmutableSet.copyOf(dataSets.keySet());
        }

        public Set<ShallowDataSet> dataSets() {
            return ImmutableSet.copyOf(dataSets.values());
        }

        public Set<Integer> declaredTemplateIds() {
            return ImmutableSet.copyOf(templates.keySet());
        }

        public Set<ShallowTemplateSet.Record> templateRecords() {
            return ImmutableSet.copyOf(templates.values());
        }

        public Set<Integer> declaredOptionsTemplateIds() {
            return ImmutableSet.copyOf(optionsTemplates.keySet());
        }

        public Set<ShallowOptionsTemplateSet.Record> optionsTemplateRecords() {
            return ImmutableSet.copyOf(optionsTemplates.values());
        }
    }
}
