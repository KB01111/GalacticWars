package galacticwars.clonewars.data

/** Shared validation for saved-data schemas that support migration from older versions. */
object SavedDataSchemaPolicy {
    @JvmStatic
    fun migrate(persistedVersion: Int, currentVersion: Int, domain: String): Int {
        require(persistedVersion >= 0) {
            "Invalid $domain schema $persistedVersion"
        }
        require(persistedVersion <= currentVersion) {
            "Unsupported $domain schema $persistedVersion"
        }
        return currentVersion
    }
}
