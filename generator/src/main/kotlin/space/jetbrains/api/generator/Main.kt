package space.jetbrains.api.generator

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.kotlin.*
import space.jetbrains.api.generator.HA_UrlParameterOption.Const
import space.jetbrains.api.generator.HA_UrlParameterOption.Var
import space.jetbrains.api.generator.HierarchyRole.FINAL
import space.jetbrains.api.generator.HierarchyRole.SEALED
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

class HttpApiEntitiesById private constructor(
    val dtoAndUrlParams: Map<TID, HA_Dto>,
    val enums: Map<TID, HA_Enum>,
    val urlParams: Map<TID, HA_UrlParameter>,
    val resources: Map<TID, HA_Resource>,
    val menuIds: List<HA_MenuId>,
) {
    constructor(model: HA_Model) : this(
        dtoAndUrlParams = model.dto.associateBy { it.id } + model.urlParams.flatMap { it.toDtos() },
        enums = model.enums.associateBy { it.id },
        urlParams = model.urlParams.associateBy { it.id },
        resources = model.resources.asSequence().flatMap { dfs(it, HA_Resource::nestedResources) }.associateBy { it.id },
        menuIds = model.menuIds
    )
}

private fun HA_UrlParameter.toDtos(): Iterable<Pair<TID, HA_Dto>> {
    return options.map {
        val optionId = it.optionName.toLowerCase()
        optionId to HA_Dto(
            id = optionId,
            name = it.optionName,
            fields = when (it) {
                is Const -> emptyList()
                is Var -> listOf(HA_DtoField(it.parameter, extension = false))
            },
            hierarchyRole = FINAL,
            extends = HA_Dto.Ref(id),
            implements = emptyList(),
            inheritors = emptyList(),
            deprecation = it.deprecation,
            record = false
        )
    } + (id to HA_Dto(
        id = id,
        name = name,
        fields = emptyList(),
        hierarchyRole = SEALED,
        extends = null,
        implements = emptyList(),
        inheritors = options.map { HA_Dto.Ref(it.optionName.toLowerCase()) },
        deprecation = deprecation,
        record = false
    ))
}

fun main(vararg args: String) {
    require(args.size in 2..3 && args.getOrNull(2)?.equals("--no-cleanup") != false) {
        // GET /api/http/http-api-model?$fields=dto(id,deprecation,extends,fields,hierarchyRole,implements,inheritors,
        // name,record),enums(id,deprecation,name,values),resources(id,displayPlural,displaySingular,endpoints,
        // nestedResources!,parentResource,path)
        "HTTP Client Generator accepts two or three arguments: path to HTTP model, path to output directory and, " +
            "optionally, '--no-cleanup'"
    }

    Log.info { "Parsing HTTP model" }
    val model = HttpApiEntitiesById(jackson.readValue(File(args[0])))

    val out = File(args[1])
    if (out.exists() && args.size != 3) out.deleteRecursively()
    out.mkdirs()

    Log.info { "Generating types for SDK" }
    val generatedTypes = generateTypes(model)
    Log.info { "Generating resources for SDK" }
    val generatedResources = generateResources(model)
    Log.info { "Generating structures for SDK" }
    val generatedStructures = generateStructures(model)
    Log.info { "Generating partials for SDK" }
    val generatedPartials = generatePartials(model)
    Log.info { "Generating menu IDs for SDK" }
    val generatedMenuIds = generateMenuIds(model)

    (generatedTypes + generatedResources + generatedStructures + generatedPartials + generatedMenuIds).forEach {
        it.writeTo(out)
    }
}
