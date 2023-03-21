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
package org.graylog.plugins.threatintel.whois.ip.parsers;

import org.graylog.plugins.threatintel.whois.ip.InternetRegistry;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ARINResponseParserTest {

    private static final String MATCH = "#\n" +
            "# ARIN WHOIS data and services are subject to the Terms of Use\n" +
            "# available at: https://www.arin.net/whois_tou.html\n" +
            "#\n" +
            "# If you see inaccuracies in the results, please report at\n" +
            "# https://www.arin.net/public/whoisinaccuracy/index.xhtml\n" +
            "#\n" +
            "\n" +
            "\n" +
            "#\n" +
            "# Query terms are ambiguous.  The query is assumed to be:\n" +
            "#     \"n 99.42.44.219\"\n" +
            "#\n" +
            "# Use \"?\" to get help.\n" +
            "#\n" +
            "\n" +
            "#\n" +
            "# The following results may also be obtained via:\n" +
            "# https://whois.arin.net/rest/nets;q=99.42.44.219?showDetails=true&showARIN=false&showNonArinTopLevelNet=false&ext=netref2\n" +
            "#\n" +
            "\n" +
            "NetRange:       99.0.0.0 - 99.127.255.255\n" +
            "CIDR:           99.0.0.0/9\n" +
            "NetName:        SBCIS-SBIS\n" +
            "NetHandle:      NET-99-0-0-0-1\n" +
            "Parent:         NET99 (NET-99-0-0-0-0)\n" +
            "NetType:        Direct Allocation\n" +
            "OriginAS:       AS7132\n" +
            "Organization:   AT&T Internet Services (SIS-80)\n" +
            "RegDate:        2008-02-25\n" +
            "Updated:        2012-03-02\n" +
            "Comment:        Contact support@swbell.net for technical supportissues\n" +
            "Comment:        For policy abuse Issues contact abuse@sbcglobal.net\n" +
            "Comment:        For Law Enforcement Requests for Information Fax or E-mail\n" +
            "Comment:        130 E TRAVIS ST. Rm. 3P01, San Antonio, TX\n" +
            "Comment:        78205-1601\n" +
            "Comment:        Fax Number: (210)370-1073\n" +
            "Ref:            https://whois.arin.net/rest/net/NET-99-0-0-0-1\n" +
            "\n" +
            "\n" +
            "\n" +
            "OrgName:        AT&T Internet Services\n" +
            "OrgId:          SIS-80\n" +
            "Address:        3300 E Renner Rd\n" +
            "Address:        Mailroom B2139 \n" +
            "Address:        Attn:IP Management\n" +
            "City:           Richardson\n" +
            "StateProv:      TX\n" +
            "PostalCode:     75082\n" +
            "Country:        US\n" +
            "RegDate:        2000-06-20\n" +
            "Updated:        2016-06-17\n" +
            "Comment:        For policy abuse issues contact abuse@att.net\n" +
            "Comment:        For all subpoena, Internet, court order related matters and emergency requests contact\n" +
            "Comment:        11760 US Highway 1\n" +
            "Comment:        North Palm Beach, FL 33408  \n" +
            "Comment:        Main Number:  800-635-6840  \n" +
            "Comment:        Fax: 888-938-4715\n" +
            "Ref:            https://whois.arin.net/rest/org/SIS-80\n" +
            "\n" +
            "\n" +
            "OrgNOCHandle: SUPPO-ARIN\n" +
            "OrgNOCName:   Support ATT Internet Services\n" +
            "OrgNOCPhone:  +1-888-510-5545 \n" +
            "OrgNOCEmail:  ipadmin@att.com\n" +
            "OrgNOCRef:    https://whois.arin.net/rest/poc/SUPPO-ARIN\n" +
            "\n" +
            "OrgTechHandle: IPADM2-ARIN\n" +
            "OrgTechName:   IPAdmin ATT Internet Services\n" +
            "OrgTechPhone:  +1-888-510-5545 \n" +
            "OrgTechEmail:  ipadmin@att.com\n" +
            "OrgTechRef:    https://whois.arin.net/rest/poc/IPADM2-ARIN\n" +
            "\n" +
            "OrgAbuseHandle: ABUSE6-ARIN\n" +
            "OrgAbuseName:   Abuse ATT Internet Services\n" +
            "OrgAbusePhone:  +1-919-319-8167 \n" +
            "OrgAbuseEmail:  abuse@att.net\n" +
            "OrgAbuseRef:    https://whois.arin.net/rest/poc/ABUSE6-ARIN\n" +
            "\n" +
            "RNOCHandle: SUPPO-ARIN\n" +
            "RNOCName:   Support ATT Internet Services\n" +
            "RNOCPhone:  +1-888-510-5545 \n" +
            "RNOCEmail:  ipadmin@att.com\n" +
            "RNOCRef:    https://whois.arin.net/rest/poc/SUPPO-ARIN\n" +
            "\n" +
            "RAbuseHandle: ABUSE6-ARIN\n" +
            "RAbuseName:   Abuse ATT Internet Services\n" +
            "RAbusePhone:  +1-919-319-8167 \n" +
            "RAbuseEmail:  abuse@att.net\n" +
            "RAbuseRef:    https://whois.arin.net/rest/poc/ABUSE6-ARIN\n" +
            "\n" +
            "RTechHandle: IPADM2-ARIN\n" +
            "RTechName:   IPAdmin ATT Internet Services\n" +
            "RTechPhone:  +1-888-510-5545 \n" +
            "RTechEmail:  ipadmin@att.com\n" +
            "RTechRef:    https://whois.arin.net/rest/poc/IPADM2-ARIN\n" +
            "\n" +
            "\n" +
            "#\n" +
            "# ARIN WHOIS data and services are subject to the Terms of Use\n" +
            "# available at: https://www.arin.net/whois_tou.html\n" +
            "#\n" +
            "# If you see inaccuracies in the results, please report at\n" +
            "# https://www.arin.net/public/whoisinaccuracy/index.xhtml\n" +
            "#\n";

    private static final String REDIRECT_TO_RIPENCC = "#\n" +
            "# ARIN WHOIS data and services are subject to the Terms of Use\n" +
            "# available at: https://www.arin.net/whois_tou.html\n" +
            "#\n" +
            "# If you see inaccuracies in the results, please report at\n" +
            "# https://www.arin.net/public/whoisinaccuracy/index.xhtml\n" +
            "#\n" +
            "\n" +
            "\n" +
            "#\n" +
            "# Query terms are ambiguous.  The query is assumed to be:\n" +
            "#     \"n 138.201.14.212\"\n" +
            "#\n" +
            "# Use \"?\" to get help.\n" +
            "#\n" +
            "\n" +
            "#\n" +
            "# The following results may also be obtained via:\n" +
            "# https://whois.arin.net/rest/nets;q=138.201.14.212?showDetails=true&showARIN=false&showNonArinTopLevelNet=false&ext=netref2\n" +
            "#\n" +
            "\n" +
            "NetRange:       138.198.0.0 - 138.201.255.255\n" +
            "CIDR:           138.198.0.0/15, 138.200.0.0/15\n" +
            "NetName:        RIPE-ERX-138-198-0-0\n" +
            "NetHandle:      NET-138-198-0-0-1\n" +
            "Parent:         NET138 (NET-138-0-0-0-0)\n" +
            "NetType:        Early Registrations, Transferred to RIPE NCC\n" +
            "OriginAS:       \n" +
            "Organization:   RIPE Network Coordination Centre (RIPE)\n" +
            "RegDate:        2003-12-11\n" +
            "Updated:        2003-12-11\n" +
            "Comment:        These addresses have been further assigned to users in\n" +
            "Comment:        the RIPE NCC region.  Contact information can be found in\n" +
            "Comment:        the RIPE database at http://www.ripe.net/whois\n" +
            "Ref:            https://whois.arin.net/rest/net/NET-138-198-0-0-1\n" +
            "\n" +
            "ResourceLink:  https://apps.db.ripe.net/search/query.html\n" +
            "ResourceLink:  whois.ripe.net\n" +
            "\n" +
            "OrgName:        RIPE Network Coordination Centre\n" +
            "OrgId:          RIPE\n" +
            "Address:        P.O. Box 10096\n" +
            "City:           Amsterdam\n" +
            "StateProv:      \n" +
            "PostalCode:     1001EB\n" +
            "Country:        NL\n" +
            "RegDate:        \n" +
            "Updated:        2013-07-29\n" +
            "Ref:            https://whois.arin.net/rest/org/RIPE\n" +
            "\n" +
            "ReferralServer:  whois://whois.ripe.net\n" +
            "ResourceLink:  https://apps.db.ripe.net/search/query.html\n" +
            "\n" +
            "OrgTechHandle: RNO29-ARIN\n" +
            "OrgTechName:   RIPE NCC Operations\n" +
            "OrgTechPhone:  +31 20 535 4444 \n" +
            "OrgTechEmail:  hostmaster@ripe.net\n" +
            "OrgTechRef:    https://whois.arin.net/rest/poc/RNO29-ARIN\n" +
            "\n" +
            "OrgAbuseHandle: ABUSE3850-ARIN\n" +
            "OrgAbuseName:   Abuse Contact\n" +
            "OrgAbusePhone:  +31205354444 \n" +
            "OrgAbuseEmail:  abuse@ripe.net\n" +
            "OrgAbuseRef:    https://whois.arin.net/rest/poc/ABUSE3850-ARIN\n" +
            "\n" +
            "\n" +
            "#\n" +
            "# ARIN WHOIS data and services are subject to the Terms of Use\n" +
            "# available at: https://www.arin.net/whois_tou.html\n" +
            "#\n" +
            "# If you see inaccuracies in the results, please report at\n" +
            "# https://www.arin.net/public/whoisinaccuracy/index.xhtml\n" +
            "#\n";

    private static String TWO_MATCH = "#\n" +
            "# ARIN WHOIS data and services are subject to the Terms of Use\n" +
            "# available at: https://www.arin.net/resources/registry/whois/tou/\n" +
            "#\n" +
            "# If you see inaccuracies in the results, please report at\n" +
            "# https://www.arin.net/resources/registry/whois/inaccuracy_reporting/\n" +
            "#\n" +
            "# Copyright 1997-2020, American Registry for Internet Numbers, Ltd.\n" +
            "#\n" +
            "\n" +
            "\n" +
            "\n" +
            "# start\n" +
            "\n" +
            "NetRange:       24.248.0.0 - 24.255.255.255\n" +
            "CIDR:           24.248.0.0/13\n" +
            "NetName:        NETBLK-COX-ATLANTA-8\n" +
            "NetHandle:      NET-24-248-0-0-1\n" +
            "Parent:         NET24 (NET-24-0-0-0-0)\n" +
            "NetType:        Direct Allocation\n" +
            "OriginAS:\n" +
            "Organization:   Cox Communications Inc. (CXA)\n" +
            "RegDate:        2003-10-28\n" +
            "Updated:        2012-03-02\n" +
            "Comment:        For legal requests/assistance please use the following contact information:\n" +
            "Comment:\n" +
            "Comment:        Cox Subpoena Phone: 404-269-0100\n" +
            "Comment:\n" +
            "Comment:        Cox Subpoena Info: http://www.cox.com/policy/leainformation/default.asp\n" +
            "Ref:            https://rdap.arin.net/registry/ip/24.248.0.0\n" +
            "\n" +
            "\n" +
            "\n" +
            "OrgName:        Cox Communications Inc.\n" +
            "OrgId:          CXA\n" +
            "Address:        1400 Lake Hearn Dr.\n" +
            "City:           Atlanta\n" +
            "StateProv:      GA\n" +
            "PostalCode:     30319\n" +
            "Country:        FOO\n" +
            "RegDate:\n" +
            "Updated:        2019-05-24\n" +
            "Comment:        For legal requests/assistance please use the\n" +
            "Comment:        following contact information:\n" +
            "Comment:        Cox Subpoena Info: https://www.cox.com/aboutus/policies/law-enforcement-and-subpoenas-information.html\n" +
            "Ref:            https://rdap.arin.net/registry/entity/CXA\n" +
            "\n" +
            "\n" +
            "OrgTechHandle: GOODW243-ARIN\n" +
            "OrgTechName:   Goodwin, Mark\n" +
            "OrgTechPhone:  +1-404-269-4416\n" +
            "OrgTechEmail:  mark.goodwin@cox.com\n" +
            "OrgTechRef:    https://rdap.arin.net/registry/entity/GOODW243-ARIN\n" +
            "\n" +
            "OrgTechHandle: ADA131-ARIN\n" +
            "OrgTechName:   Anderson, Alvin Demond\n" +
            "OrgTechPhone:  +1-404-269-4416\n" +
            "OrgTechEmail:  alvin.anderson@cox.com\n" +
            "OrgTechRef:    https://rdap.arin.net/registry/entity/ADA131-ARIN\n" +
            "\n" +
            "OrgTechHandle: MEROL3-ARIN\n" +
            "OrgTechName:   Merola, Cari\n" +
            "OrgTechPhone:  +1-404-269-4416\n" +
            "OrgTechEmail:  cari.merola@cox.com\n" +
            "OrgTechRef:    https://rdap.arin.net/registry/entity/MEROL3-ARIN\n" +
            "\n" +
            "OrgAbuseHandle: IC146-ARIN\n" +
            "OrgAbuseName:   Cox Communications Inc\n" +
            "OrgAbusePhone:  +1-404-269-7626\n" +
            "OrgAbuseEmail:  abuse@cox.net\n" +
            "OrgAbuseRef:    https://rdap.arin.net/registry/entity/IC146-ARIN\n" +
            "\n" +
            "OrgTechHandle: IPADM754-ARIN\n" +
            "OrgTechName:   IP Adminstrator\n" +
            "OrgTechPhone:  +1-404-269-0188\n" +
            "OrgTechEmail:  sophea.long@cox.com\n" +
            "OrgTechRef:    https://rdap.arin.net/registry/entity/IPADM754-ARIN\n" +
            "\n" +
            "OrgTechHandle: BERUB3-ARIN\n" +
            "OrgTechName:   Berube, Tori\n" +
            "OrgTechPhone:  +1-404-269-4416\n" +
            "OrgTechEmail:  tori.berube@cox.com\n" +
            "OrgTechRef:    https://rdap.arin.net/registry/entity/BERUB3-ARIN\n" +
            "\n" +
            "# end\n" +
            "\n" +
            "\n" +
            "# start\n" +
            "\n" +
            "NetRange:       24.255.128.0 - 24.255.255.255\n" +
            "CIDR:           24.255.128.0/17\n" +
            "NetName:        NETBLK-WI-RDC-24-255-128-0\n" +
            "NetHandle:      NET-24-255-128-0-1\n" +
            "Parent:         NETBLK-COX-ATLANTA-8 (NET-24-248-0-0-1)\n" +
            "NetType:        Reassigned\n" +
            "OriginAS:\n" +
            "Customer:       Cox Communications (C00898431)\n" +
            "RegDate:        2004-08-31\n" +
            "Updated:        2004-08-31\n" +
            "Ref:            https://rdap.arin.net/registry/ip/24.255.128.0\n" +
            "\n" +
            "\n" +
            "CustName:       Cox Communications\n" +
            "Address:        1400 Lake Hearn Dr.\n" +
            "City:           Atlanta\n" +
            "StateProv:      GA\n" +
            "PostalCode:     30319\n" +
            "Country:        US\n" +
            "RegDate:        2004-08-31\n" +
            "Updated:        2011-03-19\n" +
            "Ref:            https://rdap.arin.net/registry/entity/C00898431\n" +
            "\n" +
            "OrgTechHandle: GOODW243-ARIN\n" +
            "OrgTechName:   Goodwin, Mark\n" +
            "OrgTechPhone:  +1-404-269-4416\n" +
            "OrgTechEmail:  mark.goodwin@cox.com\n" +
            "OrgTechRef:    https://rdap.arin.net/registry/entity/GOODW243-ARIN\n" +
            "\n" +
            "OrgTechHandle: ADA131-ARIN\n" +
            "OrgTechName:   Anderson, Alvin Demond\n" +
            "OrgTechPhone:  +1-404-269-4416\n" +
            "OrgTechEmail:  alvin.anderson@cox.com\n" +
            "OrgTechRef:    https://rdap.arin.net/registry/entity/ADA131-ARIN\n" +
            "\n" +
            "OrgTechHandle: MEROL3-ARIN\n" +
            "OrgTechName:   Merola, Cari\n" +
            "OrgTechPhone:  +1-404-269-4416\n" +
            "OrgTechEmail:  cari.merola@cox.com\n" +
            "OrgTechRef:    https://rdap.arin.net/registry/entity/MEROL3-ARIN\n" +
            "\n" +
            "OrgAbuseHandle: IC146-ARIN\n" +
            "OrgAbuseName:   Cox Communications Inc\n" +
            "OrgAbusePhone:  +1-404-269-7626\n" +
            "OrgAbuseEmail:  abuse@cox.net\n" +
            "OrgAbuseRef:    https://rdap.arin.net/registry/entity/IC146-ARIN\n" +
            "\n" +
            "OrgTechHandle: IPADM754-ARIN\n" +
            "OrgTechName:   IP Adminstrator\n" +
            "OrgTechPhone:  +1-404-269-0188\n" +
            "OrgTechEmail:  sophea.long@cox.com\n" +
            "OrgTechRef:    https://rdap.arin.net/registry/entity/IPADM754-ARIN\n" +
            "\n" +
            "OrgTechHandle: BERUB3-ARIN\n" +
            "OrgTechName:   Berube, Tori\n" +
            "OrgTechPhone:  +1-404-269-4416\n" +
            "OrgTechEmail:  tori.berube@cox.com\n" +
            "OrgTechRef:    https://rdap.arin.net/registry/entity/BERUB3-ARIN\n" +
            "\n" +
            "# end\n" +
            "\n" +
            "\n" +
            "\n" +
            "#\n" +
            "# ARIN WHOIS data and services are subject to the Terms of Use\n" +
            "# available at: https://www.arin.net/resources/registry/whois/tou/\n" +
            "#\n" +
            "# If you see inaccuracies in the results, please report at\n" +
            "# https://www.arin.net/resources/registry/whois/inaccuracy_reporting/\n" +
            "#\n" +
            "# Copyright 1997-2020, American Registry for Internet Numbers, Ltd.\n" +
            "#";

    private static String FOUR_MATCH = "#\n" +
            "# start\n" +
            "NetType:        Direct Assignment\n" +
            "Organization:   Direct Assignment Org\n" +
            "Country:        FOO\n" +
            "# end\n" +
            "# start\n" +
            "NetType:        Direct Allocation\n" +
            "Organization:   Direct Allocation Org\n" +
            "Country:        BAR\n" +
            "# end\n" +
            "# start\n" +
            "NetType:        Reassigned\n" +
            "Customer:       Reassigned Customer\n" +
            "Country:        BAZ\n" +
            "# end\n" +
            "# start\n" +
            "NetType:        Reallocated\n" +
            "Customer:       Reallocated Customer\n" +
            "Country:        MOO\n" +
            "# end\n";

    @Test
    public void testRunDirectMatch() throws Exception {
        ARINResponseParser parser = new ARINResponseParser();
        for (String line : MATCH.split("\n")) {
            parser.readLine(line);
        }

        assertFalse(parser.isRedirect());
        assertNull(parser.getRegistryRedirect());

        assertEquals("US", parser.getCountryCode());
        assertEquals("AT&T Internet Services (SIS-80)", parser.getOrganization());
    }

    @Test
    public void testRunTwoMatches() throws Exception {
        ARINResponseParser parser = new ARINResponseParser();
        for (String line : TWO_MATCH.split("\n")) {
            parser.readLine(line);
        }

        assertFalse(parser.isRedirect());
        assertNull(parser.getRegistryRedirect());

        assertEquals("US", parser.getCountryCode());
        assertEquals("Cox Communications (C00898431)", parser.getOrganization());
    }

    @Test
    public void testRunFourMatches() throws Exception {
        ARINResponseParser parser = new ARINResponseParser();
        for (String line : FOUR_MATCH.split("\n")) {
            parser.readLine(line);
        }

        assertFalse(parser.isRedirect());
        assertNull(parser.getRegistryRedirect());

        assertEquals("BAZ", parser.getCountryCode());
        assertEquals("Reassigned Customer", parser.getOrganization());
    }

    @Test
    public void testRunRedirect() throws Exception {
        ARINResponseParser parser = new ARINResponseParser();
        for (String line : REDIRECT_TO_RIPENCC.split("\n")) {
            parser.readLine(line);
        }

        assertTrue(parser.isRedirect());
        assertEquals(InternetRegistry.RIPENCC, parser.getRegistryRedirect());
    }

}
