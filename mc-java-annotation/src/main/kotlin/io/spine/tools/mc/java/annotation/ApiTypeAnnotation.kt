package io.spine.tools.mc.java.annotation

import io.spine.protodata.codegen.java.ClassOrEnumName
import io.spine.protodata.codegen.java.annotation.TypeAnnotation
import io.spine.protodata.renderer.SourceFile

/**
 * Annotates a type with an annotation of the given class.
 */
internal class ApiTypeAnnotation<T : Annotation>(
    subject: ClassOrEnumName,
    annotationClass: Class<T>
) :
    TypeAnnotation<T>(annotationClass, subject) {

    override fun renderAnnotationArguments(file: SourceFile): String = ""
}
