package space.jetbrains.api.generator

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.kotlin.*
import java.io.*

object Log {
    inline fun info(message: () -> String) {
        println(message())
    }
}

private val jackson: ObjectMapper = ObjectMapper()
    .configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
    .setSerializationInclusion(JsonInclude.Include.NON_NULL)
    .registerModule(KotlinModule())
    .registerModule(JodaModule())

class HttpApiEntitiesById(
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
        // GET /api/http/http-api-model?$fields=dto(id,deprecation,extends,fields,hierarchyRole,implements,inheritors,
        // name,record),enums(id,deprecation,name,values),resources(id,displayPlural,displaySingular,endpoints,
        // nestedResources!,parentResource,path)
        "HTTP Client Generator accepts a two arguments: path to HTTP model and path to output directory"
    }

    Log.info { "Parsing HTTP model" }
    val model = HttpApiEntitiesById(jackson.readValue(File(args[0])))

    val out = File(args[1])
    if (out.exists()) out.deleteRecursively()
    out.mkdirs()

    Log.info { "Generating types for HTTP API Client" }
    val generatedTypes = generateTypes(model)
    Log.info { "Generating resources for HTTP API Client" }
    val generatedResources = generateResources(model)
    Log.info { "Generating structures for HTTP API Client" }
    val generatedStructures = generateStructures(model)

    (generatedTypes + generatedResources + generatedStructures).forEach {
        it.writeTo(out)
    }
}
