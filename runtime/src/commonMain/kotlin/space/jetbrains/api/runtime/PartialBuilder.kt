package space.jetbrains.api.runtime

import space.jetbrains.api.runtime.TypeStructure.Property
import space.jetbrains.api.runtime.PartialBuilder.Requested.*
import space.jetbrains.api.runtime.PartialBuilder.Special.BATCH
import space.jetbrains.api.runtime.PartialBuilder.Special.MAP

@DslMarker
annotation class PartialQueryDsl

@PartialQueryDsl
interface Partial {
    fun defaultPartial()
}

abstract class PartialImpl(protected val builder: PartialBuilder) : Partial {
    override fun defaultPartial() {
        builder.addDefault()
    }
}

class PartialBuilder(
    private val special: Special? = null,
    private val parent: PartialBuilder? = null
) {
    sealed class Requested {
        abstract val partial: PartialBuilder?

        class Partially(override val partial: PartialBuilder) : Requested()

        object Full : Requested() {
            override val partial: Nothing? = null
        }

        class Recursive(val depth: Int, sameAs: PartialBuilder) : Requested() {
            override val partial: PartialBuilder = sameAs
        }
    }

    val requestedProperties: MutableMap<String, Requested> = mutableMapOf()

    private fun merge(name: String, requested: Requested) {
        when (val old = requestedProperties[name]) {
            null, Full -> requestedProperties[name] = requested
            is Recursive -> error("Recursive partials cannot be merged")
            is Partially -> when (requested) {
                Full -> return
                is Recursive -> error("Recursive partials cannot be merged")
                is Partially -> requested.partial.requestedProperties.forEach { (nestedName, nested) ->
                    old.partial.merge(nestedName, nested)
                }
            }
        }.let {}
    }

    fun add(propertyName: String): Unit = merge(propertyName, Full)

    fun add(
        propertyName: String,
        buildPartial: (PartialBuilder) -> Unit,
        special: Special? = null
    ) {
        merge(propertyName, Partially(PartialBuilder(special, this).also(buildPartial)))
    }

    fun <T : Partial> add(
        propertyName: String,
        providePartial: (PartialBuilder) -> T,
        buildPartial: T.() -> Unit,
        special: Special? = null
    ) {
        merge(propertyName, Partially(PartialBuilder(special, this).also {
            providePartial(it).buildPartial()
        }))
    }

    fun addRecursively(
        propertyName: String,
        sameAs: PartialBuilder
    ) {
        merge(propertyName, Recursive(findAncestor(this, sameAs), sameAs))
    }

    fun addDefault() = add("*")

    fun buildQuery(): String {
        val query = requestedProperties.entries.joinToString(",") { (name, partial) ->
            name + when (partial) {
                is Partially -> "(" + partial.partial.buildQuery() + ")"
                is Recursive -> "!" + partial.depth.takeIf { it != 1 }?.toString().orEmpty()
                else -> ""
            }
        }
        return when (special) {
            MAP -> "key,value($query)"
            BATCH -> "next,totalCount,data($query)"
            null -> query
        }
    }

    enum class Special {
        MAP, BATCH
    }

    private companion object {
        tailrec fun findAncestor(partial: PartialBuilder, ancestor: PartialBuilder, currentDepth: Int = 0): Int {
            return if (partial === ancestor) currentDepth
            else findAncestor(
                partial = partial.parent ?: throw NoSuchElementException("Ancestor not found"),
                ancestor = ancestor,
                currentDepth = currentDepth + if (partial.special != null) 2 else 1
            )
        }
    }
}
