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

package io.spine.tools.psi.java

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.lang.MetaLanguage
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.extensions.ExtensionsArea
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiModifierList
import com.intellij.psi.augment.PsiAugmentProvider

public fun PsiClass.addFirst(element: PsiElement): PsiElement {
    return addBefore(element, firstChild)
}

public val PsiClass.modifiers: PsiModifierList
    get() {
        check(modifierList != null) {
            "The class $this does not have modifiers"
        }
        return modifierList!!
    }

public fun PsiClass.makePublic(): PsiClass {
    modifiers.setIfAbsent(PsiModifier.PUBLIC)
    return this
}

public fun PsiClass.makeStatic(): PsiClass {
    modifiers.setIfAbsent(PsiModifier.STATIC)
    return this
}

public fun PsiClass.makeFinal(): PsiClass {
    modifiers.setIfAbsent(PsiModifier.FINAL)
    return this
}

private fun PsiModifierList.setIfAbsent(modifier: String) {
    if (!hasModifierProperty(modifier)) {
        setModifierProperty(modifier, true)
    }
}

public object MetaLanguageSupport {

    private var configured: Boolean = false

    public fun setUp() {
        if (configured) {
            return
        }
        registerInArea(Extensions.getRootArea())
        registerInArea(ApplicationManager.getApplication().extensionArea)
        
        configured = true
    }

    private fun registerInArea(extensionArea: ExtensionsArea?) {
        if(extensionArea == null) {
            return
        }
        CoreApplicationEnvironment.registerExtensionPoint(
            extensionArea, MetaLanguage.EP_NAME, MetaLanguage::class.java
        )
        CoreApplicationEnvironment.registerExtensionPoint(
            extensionArea, PsiAugmentProvider.EP_NAME, PsiAugmentProvider::class.java
        )
    }
}
