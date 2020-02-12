package space.jetbrains.api.generator

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.kotlin.*
import java.io.*

object log {
    inline fun info(message: () -> String) {
        println(message())
    }
}

val jackson: ObjectMapper = ObjectMapper()
    .configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
    .setSerializationInclusion(JsonInclude.Include.NON_NULL)
//    .enableDefaultTyping()
    .registerModule(KotlinModule())
    .registerModule(JodaModule())

class SelfContainedHA_Model(
    val dto: Map<TID, HA_Dto>,
    val enums: Map<TID, HA_Enum>,
    val resources: Map<TID, HA_Resource>
) {
    constructor(model: HA_Model) : this(
        dto = model.dto.associateBy { it.id },
        enums = model.enums.associateBy { it.id },
        resources = model.resources.asSequence().flatMap { dfs(it, HA_Resource::nestedResources) }.associateBy { it.id }
    )
}

fun main(vararg args: String) {
    require(args.size == 2) {
        "HTTP Client Generator accepts a two arguments: path to serialized HTTP model and path to output directory"
    }

    log.info { "Parsing HTTP model" }
    val model = SelfContainedHA_Model(jackson.readValue(File(args[0])))

    val out = File(args[1])
    if (out.exists()) out.deleteRecursively()
    out.mkdirs()

    log.info { "Generating types for HTTP API Client" }
    val generatedTypes = generateTypes(model)
    log.info { "Generating resources for HTTP API Client" }
    val generatedResources = generateResources(model)
    log.info { "Generating structures for HTTP API Client" }
    val generatedStructures = generateStructures(model)

    (generatedTypes + generatedResources + generatedStructures).forEach {
        it.writeTo(out)
    }
}
