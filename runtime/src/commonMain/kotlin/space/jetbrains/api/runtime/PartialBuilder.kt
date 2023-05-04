package space.jetbrains.api.runtime

@DslMarker
private annotation class PartialQueryDsl

@PartialQueryDsl
public interface Partial {
    public fun defaultPartial()
}

public abstract class PartialImpl(protected val builder: PartialBuilder.Explicit) : Partial {
    override fun defaultPartial() {
        builder.addDefault()
    }

    protected companion object {
        public fun getPartialBuilder(partial: Partial): PartialBuilder.Explicit = (partial as PartialImpl).builder
    }
}

public sealed class PartialBuilder(private val parent: Explicit?) {
    internal abstract val children: Map<String, PartialBuilder>?
    internal abstract val hasAllDefault: Boolean

    public class Full(parent: Explicit) : PartialBuilder(parent) {
        override val children: Nothing? get() = null
        override val hasAllDefault: Boolean get() = true
    }

    public class Recursive internal constructor(
        internal val depth: Int,
        private val sameAs: Explicit,
        parent: Explicit,
    ) : PartialBuilder(parent) {
        override val children: Map<String, PartialBuilder> get() = sameAs.children
        override val hasAllDefault: Boolean get() = sameAs.hasAllDefault
    }

    public class Explicit(
        internal val isBatch: Boolean = false,
        parent: Explicit? = null,
    ) : PartialBuilder(parent) {
        private val _children: MutableMap<String, PartialBuilder> = mutableMapOf()
        override val children: Map<String, PartialBuilder> get() = _children
        override var hasAllDefault: Boolean = false
            private set

        public fun add(propertyName: String): Unit = merge(propertyName, Full(this))

        public fun add(propertyName: String, buildPartial: (Explicit) -> Unit, isBatch: Boolean = false): Unit =
            merge(propertyName, Explicit(isBatch, this).also(buildPartial))

        public fun <T : Partial> add(
            propertyName: String,
            providePartial: (Explicit) -> T,
            buildPartial: T.() -> Unit,
            isBatch: Boolean = false,
        ): Unit = merge(propertyName, Explicit(isBatch, this).also {
            providePartial(it).buildPartial()
        })

        public fun addRecursively(propertyName: String, sameAs: Explicit): Unit =
            merge(propertyName, Recursive(findAncestor(this, sameAs), sameAs, this))

        public fun addDefault() {
            hasAllDefault = true
        }

        private fun merge(name: String, partial: PartialBuilder) {
            when (val old = _children[name]) {
                null, is Full -> _children[name] = partial
                is Recursive -> error("Recursive partials cannot be merged")
                is Explicit -> when (partial) {
                    is Full -> return
                    is Recursive -> error("Recursive partials cannot be merged")
                    is Explicit -> partial.children.forEach { (nestedName, nested) ->
                        old.merge(nestedName, nested)
                    }
                }
            }
        }

        public fun buildQuery(): String {
            val query = children.entries.joinToString(",") { (name, partial) ->
                name + when (partial) {
                    is Explicit -> "(" + partial.buildQuery() + ")"
                    is Recursive -> "!" + partial.depth.takeIf { it != 1 }?.toString().orEmpty()
                    is Full -> ""
                }
            }
            val queryWithDefault = when {
                !hasAllDefault -> query
                query.isEmpty() -> "*"
                else -> "$query,*"
            }

            /** Both [Batch] and [SyncBatch] have `data` field, other fields are different **/
            return if (isBatch) "*,data($queryWithDefault)" else queryWithDefault
        }
    }

    private companion object {
        tailrec fun findAncestor(partial: Explicit, ancestor: Explicit, currentDepth: Int = 1): Int {
            return if (partial === ancestor) {
                currentDepth
            } else findAncestor(
                partial = (partial as PartialBuilder).parent ?: throw NoSuchElementException("Ancestor not found"),
                ancestor = ancestor,
                currentDepth = currentDepth + if (partial.isBatch) 2 else 1
            )
        }
    }
}
