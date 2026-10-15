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
package org.graylog.plugins.pipelineprocessor.compiler;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Stops emission once a method can no longer fit the JVM's 64 KiB code limit. ASM records forward jump locations in
 * 16 bits, so emitting a jump past that limit makes {@code Label.resolve} loop forever instead of failing later with
 * {@code MethodTooLargeException}.
 */
final class CodeSizeGuard extends MethodVisitor {
    private static final int MAX_CODE_SIZE = 65535;
    // the longest jump instruction we emit: opcode plus a 2-byte offset
    private static final int JUMP_SIZE = 3;

    CodeSizeGuard(MethodVisitor methodVisitor) {
        super(Opcodes.ASM9, methodVisitor);
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        ensureRoom();
        super.visitJumpInsn(opcode, label);
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
        ensureRoom();
        super.visitTableSwitchInsn(min, max, dflt, labels);
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        ensureRoom();
        super.visitLookupSwitchInsn(dflt, keys, labels);
    }

    private void ensureRoom() {
        final Label here = new Label();
        super.visitLabel(here);
        if (here.getOffset() + JUMP_SIZE > MAX_CODE_SIZE) {
            throw new UnsupportedConstructException("method-too-large");
        }
    }
}
