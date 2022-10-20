package space.jetbrains.api.runtime

/**
 * Usage:
 * ```kotlin
 * PermissionScope.build(
 *     PermissionScopeElement(
 *         context = GlobalPermissionContextIdentifier,
 *         permission = PermissionIdentifier.ViewIssues,
 *     ),
 *     PermissionScopeElement(
 *         context = ProjectPermissionContextIdentifier(ProjectIdentifier.Key("pr1")),
 *         permission = PermissionIdentifier.CreateIssues,
 *     ),
 * )
 * ```
 */
public interface PermissionScope {
    /** Returns string representation for use in auth */
    override fun toString(): String

    public companion object {
        public val All: PermissionScope = fromString("**")

        public fun fromString(string: String): PermissionScope = object : PermissionScope {
            override fun toString(): String = string
        }
    }
}

public object PermissionScopeStructure : TypeStructure<PermissionScope>(isRecord = false) {
    override fun deserialize(context: DeserializationContext): PermissionScope {
        return PermissionScope.fromString(context.requireJson().asString(context.link))
    }

    override fun serialize(value: PermissionScope): JsonValue {
        return jsonString(value.toString())
    }
}
