package space.jetbrains.api.runtime

import space.jetbrains.api.runtime.TypeStructure.Property
import space.jetbrains.api.runtime.Partial.Requested.*
import space.jetbrains.api.runtime.Partial.Special.BATCH
import space.jetbrains.api.runtime.Partial.Special.MAP

@DslMarker
annotation class PartialQueryDsl

@PartialQueryDsl
class Partial<T : Any>(
    val structure: TypeStructure<T>,
    private val special: Special? = null,
    private val parent: Partial<*>? = null
) {
    sealed class Requested {
        abstract val partial: Partial<*>?

        class Partially(override val partial: Partial<*>) : Requested()

        object Full : Requested() {
            override val partial: Nothing? = null
        }

        class Recursive(val depth: Int, sameAs: Partial<*>) : Requested() {
            override val partial: Partial<*> = sameAs
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

    fun add(property: Property<*>): Unit = merge(property.name, Full)

    fun <U : Any> add(
        property: Property<*>,
        structure: TypeStructure<U>,
        buildPartial: Partial<U>.() -> Unit,
        special: Special? = null
    ) {
        merge(property.name, Partially(Partial(structure, special, this).apply(buildPartial)))
    }

    fun addRecursively(
        property: Property<*>,
        sameAs: Partial<*>
    ) {
        merge(property.name, Recursive(findAncestor(this, sameAs), sameAs))
    }

    internal fun addImplicitPartial(property: Property<*>) {
        @Suppress("UNCHECKED_CAST")
        merge(property.name, (property.type.partialStructure() as TypeStructure<Any>?)?.let {
            Partially(Partial(it, parent = this).apply(it.defaultPartialCompact))
        } ?: Full)
    }

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
        tailrec fun findAncestor(partial: Partial<*>, ancestor: Partial<*>, currentDepth: Int = 0): Int {
            return if (partial === ancestor) currentDepth
            else findAncestor(
                partial = partial.parent ?: throw NoSuchElementException("Ancestor not found"),
                ancestor = ancestor,
                currentDepth = currentDepth + if (partial.special != null) 2 else 1
            )
        }
    }
}
