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
package org.graylog.aws.processors.instancelookup;

import com.google.common.collect.ImmutableMap;
import okhttp3.HttpUrl;
import org.graylog.aws.auth.AWSAuthProvider;
import org.graylog.aws.config.Proxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeNetworkInterfacesResponse;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceNetworkInterface;
import software.amazon.awssdk.services.ec2.model.InstancePrivateIpAddress;
import software.amazon.awssdk.services.ec2.model.NetworkInterface;
import software.amazon.awssdk.services.ec2.model.NetworkInterfacePrivateIpAddress;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.ec2.model.Tag;

import jakarta.inject.Singleton;

import java.util.List;

@Singleton
public class InstanceLookupTable {
    private static final Logger LOG = LoggerFactory.getLogger(InstanceLookupTable.class);

    enum InstanceType {
        RDS,
        EC2,
        ELB,
        UNKNOWN
    }

    private boolean loaded = false;

    private ImmutableMap<String, Instance> ec2Instances;
    private ImmutableMap<String, NetworkInterface> networkInterfaces;

    // TODO METRICS

    public void reload(List<Region> regions, AWSAuthProvider awsAuthProvider, HttpUrl proxyUrl) {
        LOG.debug("Reloading AWS instance lookup table.");

        ImmutableMap.Builder<String, Instance> ec2InstancesBuilder = ImmutableMap.<String, Instance>builder();
        ImmutableMap.Builder<String, NetworkInterface> networkInterfacesBuilder = ImmutableMap.<String, NetworkInterface>builder();

        for (Region region : regions) {
            try {
                Ec2Client ec2Client;

                if (proxyUrl != null) {
                    ec2Client = Ec2Client.builder()
                            .credentialsProvider(awsAuthProvider)
                            .region(region)
                            .httpClientBuilder(Proxy.forAWS(proxyUrl))
                            .build();
                } else {
                    ec2Client = Ec2Client.builder()
                            .credentialsProvider(awsAuthProvider)
                            .region(region)
                            .build();
                }

                // Load network interfaces
                LOG.debug("Requesting AWS network interface descriptions in [{}].", region.id());
                DescribeNetworkInterfacesResponse interfaces = ec2Client.describeNetworkInterfaces();
                for (NetworkInterface iface : interfaces.networkInterfaces()) {
                    LOG.debug("Discovered network interface [{}].", iface.networkInterfaceId());

                    // Add all private IP addresses.
                    for (final NetworkInterfacePrivateIpAddress privateIp : iface.privateIpAddresses()) {
                        LOG.debug("Network interface [{}] has private IP: {}", iface.networkInterfaceId(), privateIp);
                        networkInterfacesBuilder.put(privateIp.privateIpAddress(), iface);
                    }

                    // Add public IP address.
                    if (iface.association() != null) {
                        String publicIp = iface.association().publicIp();
                        LOG.debug("Network interface [{}] has public IP: {}", iface.networkInterfaceId(), publicIp);
                        networkInterfacesBuilder.put(publicIp, iface);
                    }
                }

                // Load EC2 instances
                LOG.debug("Requesting EC2 instance descriptions in [{}].", region.id());
                DescribeInstancesResponse ec2Result = ec2Client.describeInstances();
                for (Reservation reservation : ec2Result.reservations()) {
                    LOG.debug("Fetching instances for reservation [{}].", reservation.reservationId());
                    for (final Instance instance : reservation.instances()) {
                        LOG.debug("Discovered EC2 instance [{}].", instance.instanceId());

                        // Add all private IP addresses.
                        for (InstanceNetworkInterface iface : instance.networkInterfaces()) {
                            for (InstancePrivateIpAddress privateIp : iface.privateIpAddresses()) {
                                LOG.debug("EC2 instance [{}] has private IP: {}", instance.instanceId(), privateIp.privateIpAddress());
                                ec2InstancesBuilder.put(privateIp.privateIpAddress(), instance);
                            }
                        }

                        // Add public IP address.
                        String publicIp = instance.publicIpAddress();
                        if (publicIp != null) {
                            LOG.debug("EC2 instance [{}] has public IP: {}", instance.instanceId(), publicIp);
                            ec2InstancesBuilder.put(publicIp, instance);
                        }
                    }
                }
            } catch (Exception e) {
                LOG.error("Error when trying to refresh AWS instance lookup table in [{}]", region.id(), e);
            }
        }

        ec2Instances = ec2InstancesBuilder.build();
        networkInterfaces = networkInterfacesBuilder.build();

        this.loaded = true;
    }

    public DiscoveredInstance findByIp(String ip) {
        try {
            // Let's see if this is an EC2 instance maybe?
            if (ec2Instances.containsKey(ip)) {
                Instance instance = ec2Instances.get(ip);
                LOG.debug("Found IP [{}] in EC2 instance lookup table.", ip);
                return new DiscoveredEC2Instance(instance.instanceId(), getNameOfInstance(instance));
            }

            // If it's not an EC2 instance, we might still find it in our list of network interfaces.
            if (networkInterfaces.containsKey(ip)) {
                NetworkInterface iface = networkInterfaces.get(ip);
                switch (determineType(iface)) {
                    case RDS:
                        return new DiscoveredRDSInstance(null, null);
                    case EC2:
                        // handled directly via separate EC2 table above
                        break;
                    case ELB:
                        return new DiscoveredELBInstance(getELBNameFromInterface(iface), null);
                    case UNKNOWN:
                        LOG.debug("IP [{}] in table of network interfaces but of unknown instance type.", ip);
                        return DiscoveredInstance.UNDISCOVERED;
                }
            }

            // The IP address is not known to us. This most likely means it is an external public IP.
            return DiscoveredInstance.UNDISCOVERED;
        } catch (Exception e) {
            LOG.error("Error when trying to match IP to AWS instance. Marking as undiscovered.", e);
            return DiscoveredInstance.UNDISCOVERED;
        }
    }

    // Format is "ELB [name]" where "name" can only be a-z, A-Z and hyphens.
    private String getELBNameFromInterface(NetworkInterface iface) {
        try {
            String[] parts = iface.description().split(" ");
            if (parts.length == 2) {
                return parts[1];
            } else {
                LOG.warn("Unexpected ELB name in network interface description: [{}]", iface.description());
                return "unknown-name";
            }
        } catch (Exception e) {
            LOG.warn("Could not get ELB name from network interface description. Description was [{}]", iface.description(), e);
            return "unknown-name";
        }
    }

    /*
     * BUT I WOULD SHAVE 500 YAKS AND I WOULD SHAVE 500 MORE
     * JUST TO BE THE GIRL WHO SHAVES 1,000 YAKS TO MERGE YOUR PR
     *
     * The AWS network interface API is not very helpful and we have to get creative to
     * figure out what kind of service an interface is attached to.
     *
     * ༼ノಠل͟ಠ༽ノ ︵ ┻━┻
     */
    private InstanceType determineType(NetworkInterface iface) {
        String ownerId;
        if (iface.association() != null) {
            ownerId = iface.association().ipOwnerId();
        } else if (iface.requesterId().equals("amazon-rds")) {
            ownerId = "amazon-rds";
        } else {
            LOG.debug("AWS network interface with no association: [{}]", iface.description());
            return InstanceType.UNKNOWN;
        }

        // not using switch here because it might become nasty complicated for other instance types
        if ("amazon".equals(ownerId)) {
            return InstanceType.EC2;
        } else if ("amazon-elb".equals(ownerId)) {
            return InstanceType.ELB;
        } else if ("amazon-rds".equals(ownerId)) {
            return InstanceType.RDS;
        } else {
            return InstanceType.UNKNOWN;
        }
    }

    private String getNameOfInstance(Instance x) {
        for (Tag tag : x.tags()) {
            if ("Name".equals(tag.key())) {
                return tag.value();
            }
        }

        return null;
    }

    public boolean isLoaded() {
        return loaded;
    }

}
