package space.jetbrains.api.runtime

import space.jetbrains.api.runtime.PartialBuilder.Requested.*

@DslMarker
private annotation class PartialQueryDsl

@PartialQueryDsl
public interface Partial {
    public fun defaultPartial()
}

public abstract class PartialImpl(protected val builder: PartialBuilder) : Partial {
    override fun defaultPartial() {
        builder.addDefault()
    }

    protected companion object {
        public fun getPartialBuilder(partial: Partial): PartialBuilder = (partial as PartialImpl).builder
    }
}

public class PartialBuilder(
    private val special: Special? = null,
    private val parent: PartialBuilder? = null
) {
    public sealed class Requested {
        public abstract val partial: PartialBuilder?

        public class Partially(override val partial: PartialBuilder) : Requested()

        public object Full : Requested() {
            override val partial: Nothing? = null
        }

        public class Recursive(public val depth: Int, sameAs: PartialBuilder) : Requested() {
            override val partial: PartialBuilder = sameAs
        }
    }

    private val requestedProperties: MutableMap<String, Requested> = mutableMapOf()

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

    public fun add(propertyName: String): Unit = merge(propertyName, Full)

    public fun add(
        propertyName: String,
        buildPartial: (PartialBuilder) -> Unit,
        special: Special? = null
    ) {
        merge(propertyName, Partially(PartialBuilder(special, this).also(buildPartial)))
    }

    public fun <T : Partial> add(
        propertyName: String,
        providePartial: (PartialBuilder) -> T,
        buildPartial: T.() -> Unit,
        special: Special? = null
    ) {
        merge(propertyName, Partially(PartialBuilder(special, this).also {
            providePartial(it).buildPartial()
        }))
    }

    public fun addRecursively(
        propertyName: String,
        sameAs: PartialBuilder
    ) {
        merge(propertyName, Recursive(findAncestor(this, sameAs), sameAs))
    }

    public fun addDefault(): Unit = add("*")

    public fun buildQuery(): String {
        val query = requestedProperties.entries.joinToString(",") { (name, partial) ->
            name + when (partial) {
                is Partially -> "(" + partial.partial.buildQuery() + ")"
                is Recursive -> "!" + partial.depth.takeIf { it != 1 }?.toString().orEmpty()
                else -> ""
            }
        }
        return when (special) {
            Special.MAP -> "key,value($query)"
            Special.BATCH -> "next,totalCount,data($query)"
            null -> query
        }
    }

    public enum class Special {
        MAP, BATCH
    }

    private companion object {
        tailrec fun findAncestor(partial: PartialBuilder, ancestor: PartialBuilder, currentDepth: Int = 1): Int {
            return if (partial === ancestor) {
                currentDepth
            } else findAncestor(
                partial = partial.parent ?: throw NoSuchElementException("Ancestor not found"),
                ancestor = ancestor,
                currentDepth = currentDepth + if (partial.special != null) 2 else 1
            )
        }
    }
}
