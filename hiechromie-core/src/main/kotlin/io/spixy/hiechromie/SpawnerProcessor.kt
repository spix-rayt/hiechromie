package io.spixy.hiechromie

import java.lang.Exception
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.ExecutableType
import javax.tools.Diagnostic

@SupportedAnnotationTypes("io.spixy.hiechromie.ChromieRecord")
@SupportedSourceVersion(SourceVersion.RELEASE_11)
class SpawnerProcessor : AbstractProcessor() {
    override fun process(annotations: MutableSet<out TypeElement>, roundEnvironment: RoundEnvironment): Boolean {
        annotations.forEach { annotation ->
            val elements = roundEnvironment.getElementsAnnotatedWith(annotation)
            elements.forEach { element ->
                element.enclosedElements.forEach {
                    if(it.kind == ElementKind.CONSTRUCTOR) {
                        val parameters = (it as ExecutableElement).parameters
                        val className = (element as TypeElement).qualifiedName
                        val packageName = className.split(".").let { it.take(it.size - 1) }.joinToString(".")
                        val simpleClassName = className.split(".").last()
                        val simpleSpawnerName = simpleClassName + "ChromieRecordSpawner"
                        val spawnerName = "$packageName.$simpleSpawnerName"


                        val constructorArgumentsInjection = parameters.map { parameter ->
                            val v = parameter as VariableElement
                            "((${v.asType()}) values.get(\"${v.simpleName}\"))"
                        }.joinToString(",\n")

                        val sourceFile = processingEnv.filer.createSourceFile(spawnerName)
                        sourceFile.openWriter().use { writer ->
                            writer.write("package $packageName;\n")

                            writer.write("public class $simpleSpawnerName {\n")
                            writer.write("public $simpleClassName spawn(java.util.Map<String, Object> values) {\n")
                            writer.write("return new $simpleClassName (\n$constructorArgumentsInjection\n);}}")
                        }
                    }
                }
            }
        }

        return true
    }
}