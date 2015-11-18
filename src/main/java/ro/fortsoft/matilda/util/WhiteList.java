/*
 * Copyright (C) 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ro.fortsoft.matilda.util;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Used to test an IP address in dotted quad format against list of entries.
 * An entry can be single IPs, subnets and ranges of IP addresses.
 *
 * Not implemented:
 *   - checking of IP addresses ie not out of ipv4 range
 *   - ipv6 functionality (yeah right)
 *
 * See https://en.wikipedia.org/wiki/IP_address
 *
 * @author Decebal Suiu
 */
public class WhiteList {

    private static final String singleRegex = "^\\d+\\.\\d+\\.\\d+\\.\\d+$";
    private static final String subnetRegex = "^\\d+\\.\\d+\\.\\d+\\.\\d+/\\d+$";
    private static final String rangeRegex = "^\\d+\\.\\d+\\.\\d+\\.\\d+-\\d+\\.\\d+\\.\\d+\\.\\d+$";

    private List<String> entries;

    private Pattern singlePattern;
    private Pattern subnetPattern;
    private Pattern rangePattern;

    public WhiteList(List<String> entries) {
        this.entries = entries;

        singlePattern = Pattern.compile(singleRegex);
        subnetPattern = Pattern.compile(subnetRegex);
        rangePattern = Pattern.compile(rangeRegex);
    }

    public boolean isWhiteIp(String ip) {
        if (entries.isEmpty()) {
            return true; // ?!
        }

        // split the ip into blocks ready for processing
        int[] blocks = new int[3];
        int index = 0;
        for (int counter = 0; counter < ip.length(); counter++) {
            if (ip.charAt(counter) == '.') {
                blocks[index] = counter;
                index++;
            }
        }

        int[] ipBlocks = new int[4];
        ipBlocks[0] = Integer.parseInt(ip.substring(0,(blocks[0])));
        ipBlocks[1] = Integer.parseInt(ip.substring((blocks[0] + 1), (blocks[1])));
        ipBlocks[2] = Integer.parseInt(ip.substring((blocks[1] + 1), (blocks[2])));
        ipBlocks[3] = Integer.parseInt(ip.substring((blocks[2] + 1), (ip.length())));

        for (String entry : entries) {
            Matcher singleMatcher = singlePattern.matcher(entry);
            Matcher subnetMatcher = subnetPattern.matcher(entry);
            Matcher rangeMatcher = rangePattern.matcher(entry);

            if (singleMatcher.matches()) {
                if (entry.equals(ip)) {
                    return true;
                }
            } else if (subnetMatcher.matches()) {
                // split the ip into blocks ready for processing
                int[] range2blocks = new int[4];
                index = 0; // reuse variable
                for (int counter = 0; counter < entry.length(); counter++) {
                    if (entry.charAt(counter) == '.') {
                        range2blocks[index] = counter;
                        index++;
                    } else if (entry.charAt(counter) == '/') {
                        range2blocks[index] = counter;
                    }
                }

                int[] rangeBlocks = new int[4];
                rangeBlocks[0] = Integer.parseInt(entry.substring(0, range2blocks[0]));
                rangeBlocks[1] = Integer.parseInt(entry.substring(range2blocks[0] + 1, range2blocks[1]));
                rangeBlocks[2] = Integer.parseInt(entry.substring(range2blocks[1] + 1, range2blocks[2]));
                rangeBlocks[3] = Integer.parseInt(entry.substring(range2blocks[2] + 1, range2blocks[3]));
                int subnet2 = Integer.parseInt(entry.substring(range2blocks[3] + 1, entry.length()));

                switch (subnet2) {
                    case 8:
                        if (rangeBlocks[0] == ipBlocks[0]) {
                            return true;
                        }
                        break;
                    case 16:
                        if ((rangeBlocks[0] == ipBlocks[0]) && (rangeBlocks[1] == ipBlocks[1])) {
                            return true;
                        }
                        break;
                    case 24:
                        if ((rangeBlocks[0] == ipBlocks[0]) && (rangeBlocks[1] == ipBlocks[1]) && (rangeBlocks[2] == ipBlocks[2])) {
                            return true;
                        }
                        break;
                }
            } else if (rangeMatcher.matches()) {
                int[] indices = new int[7];
                index = 0; // reuse variable
                for (int counter = 0; counter < entry.length(); counter++) {
                    if ((entry.charAt(counter) == '.') || (entry.charAt(counter) == '-')) {
                        indices[index] = counter;
                        index++;
                    }
                }

                long[] startBlocks = new long[4];
                startBlocks[0] = Long.parseLong(entry.substring(0, indices[0]));
                startBlocks[1] = Long.parseLong(entry.substring(indices[0] + 1, indices[1]));
                startBlocks[2] = Long.parseLong(entry.substring(indices[1] + 1, indices[2]));
                startBlocks[3] = Long.parseLong(entry.substring(indices[2] + 1, indices[3]));

                long[] endBlocks = new long[4];
                endBlocks[0] = Long.parseLong(entry.substring(indices[3] + 1, indices[4]));
                endBlocks[1] = Long.parseLong(entry.substring(indices[4] + 1, indices[5]));
                endBlocks[2] = Long.parseLong(entry.substring(indices[5] + 1, indices[6]));
                endBlocks[3] = Long.parseLong(entry.substring(indices[6] + 1, entry.length()));

                /*
                    IANA-reserved private IPv4 network ranges
                                                        Start	    End	            No. of addresses
                    24-bit block (/8 prefix, 1 × A) 	10.0.0.0	10.255.255.255	16777216
                    20-bit block (/12 prefix, 16 × B)	172.16.0.0	172.31.255.255	1048576
                    16-bit block (/16 prefix, 256 × C)	192.168.0.0	192.168.255.255	65536
                */
                long startValue = (startBlocks[0] * 16777216) + (startBlocks[1] * 65536) + (startBlocks[2] * 256) + startBlocks[3];
                long endValue = (endBlocks[0] * 16777216) + (endBlocks[1] * 65536) + (endBlocks[2] * 256) + endBlocks[3];
                long ipValue = (ipBlocks[0] * 16777216) + (ipBlocks[1] * 65536) + (ipBlocks[2] * 256) + ipBlocks[3];

                if (startValue < endValue) {
                    if ((startValue <= ipValue) && (ipValue <= endValue)) {
                        return true;
                    }
                } else {
                    if ((startValue >= ipValue) && (ipValue >= endValue)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

}
