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
package org.graylog2.shared.utilities;

import org.assertj.core.api.AbstractBooleanAssert;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ContainerRuntimeDetectionTest {
    // Since cgroups v1 is pretty much gone these days, we don't specifically test it. (hard to get test data)
    final List<String> cgroupV1Lines = List.of("0::/");
    final Path canaryPath = Path.of("/.dockerenv");

    @Nested
    class WithoutDocker {
        final List<String> cgroupV2Lines = List.of(
                "64 38 0:38 / /proc/sys/fs/binfmt_misc rw,nosuid,nodev,noexec,relatime shared:93 - binfmt_misc binfmt_misc rw",
                // There might be "*/docker/*" content in a mountinfo file for processes that don't run inside a container!
                "1915 30 0:57 / /var/lib/docker/overlay2/f0717ccfc9d9efa2f18280f81ec9aeb5c6d8247ff0e366e684a7af753a97c170/merged rw,relatime shared:997 - overlay overlay rw,lowerdir=/var/lib/docker/overlay2/l/ZU2E7TUFCZFTXGTI7UMJPNZYAH:/var/lib/docker/overlay2/l/HY5Y6NGJV35NE5YPALSHA2PQRN:/var/lib/docker/overlay2/l/CVQ5I7CNB5POL6CNMDIDIQN56N:/var/lib/docker/overlay2/l/GO2C4IEP7AUYOBUO7UQK2IYXWM:/var/lib/docker/overlay2/l/OIATLJT37GDWJCEIRWHKCVKGRW,upperdir=/var/lib/docker/overlay2/f0717ccfc9d9efa2f18280f81ec9aeb5c6d8247ff0e366e684a7af753a97c170/diff,workdir=/var/lib/docker/overlay2/f0717ccfc9d9efa2f18280f81ec9aeb5c6d8247ff0e366e684a7af753a97c170/work,nouserxattr",
                "2014 29 0:4 net:[4026533221] /run/docker/netns/ef36004a6ef8 rw shared:1070 - nsfs nsfs rw",
                "1950 29 0:65 / /run/user/1000 rw,nosuid,nodev,relatime shared:1088 - tmpfs tmpfs rw,size=6578416k,nr_inodes=1644604,mode=700,uid=1000,gid=1000,inode64",
                "1849 30 0:123 / /var/lib/docker/containers/392e95df0abad324a589d0dcf1eadfa5e2bca375369473e4d23801ce51a52c47/mounts/shm rw,nosuid,nodev,noexec,relatime shared:819 - tmpfs shm rw,size=65536k,inode64"
        );

        @Test
        void isRunningInsideContainer() {
            assertIsRunningInContainer(cgroupV1Lines, cgroupV2Lines, canaryPath).isFalse();
        }
    }

    @Nested
    class WithDocker {
        final List<String> cgroupV2Lines = List.of(
                "2072 2063 0:134 / /dev/shm rw,nosuid,nodev,noexec,relatime - tmpfs shm rw,size=65536k,inode64",
                "2073 2061 252:1 /var/lib/docker/containers/3ff5651b246362e1d9d5f2070f5d5988ffca7b8922afe0d75e4fae6e505d6597/resolv.conf /etc/resolv.conf rw,relatime - ext4 /dev/mapper/vgubuntu-root rw,errors=remount-ro",
                "2097 2061 252:1 /var/lib/docker/containers/3ff5651b246362e1d9d5f2070f5d5988ffca7b8922afe0d75e4fae6e505d6597/hostname /etc/hostname rw,relatime - ext4 /dev/mapper/vgubuntu-root rw,errors=remount-ro",
                "2098 2061 252:1 /var/lib/docker/containers/3ff5651b246362e1d9d5f2070f5d5988ffca7b8922afe0d75e4fae6e505d6597/hosts /etc/hosts rw,relatime - ext4 /dev/mapper/vgubuntu-root rw,errors=remount-ro",
                "1682 2063 0:132 /0 /dev/console rw,nosuid,noexec,relatime - devpts devpts rw,gid=5,mode=620,ptmxmode=666"
        );

        @Test
        void isRunningInsideContainer() {
            assertIsRunningInContainer(cgroupV1Lines, cgroupV2Lines, canaryPath).isTrue();
        }
    }

    @Nested
    class WithKubernetes {
        final List<String> cgroupV2Lines = List.of(
                "4442 4434 259:9 / /tmp rw,relatime - ext4 /dev/nvme7n1 rw,seclabel",
                "4443 4434 259:1 /var/lib/kubelet/pods/2015a4b3-a05b-4e4d-8fa3-699914c33ee8/etc-hosts /etc/hosts rw,noatime - xfs /dev/nvme0n1p1 rw,seclabel,attr2,inode64,logbufs=8,logbsize=32k,sunit=1024,swidth=1024,noquota",
                "4444 4435 259:1 /var/lib/kubelet/pods/2015a4b3-a05b-4e4d-8fa3-699914c33ee8/containers/graylog/d52bb0e5 /dev/termination-log rw,noatime - xfs /dev/nvme0n1p1 rw,seclabel,attr2,inode64,logbufs=8,logbsize=32k,sunit=1024,swidth=1024,noquota",
                "4445 4434 259:1 /var/lib/containerd/io.containerd.grpc.v1.cri/sandboxes/fc9476ae96a432036914d1d1fcf8f4bba1fa9c8444a3379b0c46c3971590ad04/hostname /etc/hostname rw,noatime - xfs /dev/nvme0n1p1 rw,seclabel,attr2,inode64,logbufs=8,logbsize=32k,sunit=1024,swidth=1024,noquota",
                "4446 4434 259:1 /var/lib/containerd/io.containerd.grpc.v1.cri/sandboxes/fc9476ae96a432036914d1d1fcf8f4bba1fa9c8444a3379b0c46c3971590ad04/resolv.conf /etc/resolv.conf rw,noatime - xfs /dev/nvme0n1p1 rw,seclabel,attr2,inode64,logbufs=8,logbsize=32k,sunit=1024,swidth=1024,noquota",
                "4447 4435 0:338 / /dev/shm rw,nosuid,nodev,noexec,relatime - tmpfs shm rw,seclabel,size=65536k",
                "4448 4434 259:9 /cacerts /etc/ssl/certs/java/cacerts ro,relatime - ext4 /dev/nvme7n1 rw,seclabel"
        );

        @Test
        void isRunningInsideContainer() {
            assertIsRunningInContainer(cgroupV1Lines, cgroupV2Lines, canaryPath).isTrue();
        }
    }

    @Nested
    class WithCanaryPath {
        final List<String> cgroupV2Lines = List.of(
                "4435 4434 0:376 / /dev rw,nosuid - tmpfs tmpfs rw,seclabel,size=65536k,mode=755"
        );

        @Test
        void isRunningInsideContainer(@TempDir Path tempDir) throws Exception {
            final var canaryPath = tempDir.resolve(".dockerenv");

            assertIsRunningInContainer(cgroupV1Lines, cgroupV2Lines, canaryPath).isFalse();

            Files.createFile(canaryPath);

            assertIsRunningInContainer(cgroupV1Lines, cgroupV2Lines, canaryPath).isTrue();
        }
    }

    AbstractBooleanAssert<?> assertIsRunningInContainer(List<String> cgroupV1Lines, List<String> cgroupV2Lines, Path canaryPath) {
        return assertThat(ContainerRuntimeDetection.isRunningInsideContainer(cgroupV1Lines::stream, cgroupV2Lines::stream, () -> Stream.of(canaryPath)));
    }
}
