/*
 * Copyright 2024, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Redistribution and use in source and/or binary forms, with or without
 * modification, must retain the above copyright notice and the following
 * disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package io.spine.tools.mc.java.signal

import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper
import io.spine.base.EntityState
import io.spine.protodata.File
import io.spine.protodata.MessageType
import io.spine.protodata.java.ClassName
import io.spine.protodata.renderer.SourceFile
import io.spine.tools.mc.java.MessageTypeRenderer
import io.spine.tools.mc.java.WithTypeList
import io.spine.tools.mc.java.field.FieldClassFactory
import io.spine.tools.mc.java.field.superClassName
import io.spine.tools.mc.java.settings.SignalSettings
import io.spine.tools.mc.java.settings.Signals
import io.spine.tools.psi.java.execute

/**
 * An abstract base for renders of signal messages.
 *
 * @param V
 *        the type of the view state which gathers signals of the type served by
 *        this renderer.
 * @param viewClass
 *        the view state class matching the generic parameter [V].
 */
internal abstract class SignalRenderer<V>(viewClass: Class<V>) :
    MessageTypeRenderer<V, SignalSettings>(viewClass, SignalSettings::class.java),
    SignalPluginComponent where V : EntityState<File>, V : WithTypeList {

    /**
     * The settings for the kind of signals served by this renderer,
     * obtained from [settings].
     */
    protected abstract val typeSettings: Signals

    override val enabledBySettings: Boolean
        get() = typeSettings != Signals.getDefaultInstance()

    private val fieldSupertype: ClassName by lazy {
        typeSettings.generateFields.superClassName
    }

    @OverridingMethodsMustInvokeSuper
    override fun doRender(type: MessageType, sourceFile: SourceFile) {
        //TODO:2024-05-21:alexander.yevsyukov: Can we move it to `enabledBySettings` instead?
        if (typeSettings.generateFields.hasSuperclass()) {
            execute {
                val factory = FieldClassFactory(type, fieldSupertype, typeSystem!!)
                factory.render(sourceFile)
            }
        }
    }
}
