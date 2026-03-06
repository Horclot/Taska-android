package com.horclotapp.taska.spaces.data

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.horclotapp.taska.spaces.model.LoadSpacesResult
import com.horclotapp.taska.spaces.model.Space
import com.horclotapp.taska.spaces.model.SpaceMember
import com.horclotapp.taska.spaces.model.SpaceRole
import com.horclotapp.taska.spaces.model.SpaceSettings
import com.horclotapp.taska.spaces.model.SpaceStats
import com.horclotapp.taska.spaces.model.RolesSetupData
import com.horclotapp.taska.spaces.model.UserSpaceSummary
import com.horclotapp.taska.spaces.util.RoleTemplates
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await

class SpacesFirestoreService(
    private val firestore: FirebaseFirestore
) {

    suspend fun loadUserSpaces(userId: String): LoadSpacesResult = coroutineScope {
        val ownedSpaces = firestore.collection("spaces")
            .whereEqualTo("ownerId", userId)
            .get()
            .await()
            .documents
            .mapNotNull { document ->
                document.toObject(Space::class.java)?.copy(id = document.id)
            }
            .map { space ->
                UserSpaceSummary(
                    space = space,
                    accessRoleId = "owner",
                    isOwner = true
                )
            }

        val memberSpacesResult = runCatching {
            val membershipDocuments = firestore.collectionGroup("members")
                .whereEqualTo("uid", userId)
                .get()
                .await()
                .documents

            membershipDocuments.mapNotNull { memberDocument ->
                val spaceRef = memberDocument.reference.parent.parent ?: return@mapNotNull null
                val spaceSnapshot = spaceRef.get().await()
                val space = spaceSnapshot.toObject(Space::class.java)?.copy(id = spaceSnapshot.id)
                    ?: return@mapNotNull null

                UserSpaceSummary(
                    space = space,
                    accessRoleId = memberDocument.getString("roleId").orEmpty(),
                    isOwner = space.ownerId == userId
                )
            }
        }

        val warningMessage = memberSpacesResult.exceptionOrNull()?.let { error ->
            val firestoreError = error as? FirebaseFirestoreException
            if (firestoreError?.code == FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                "Создайте индекс Firestore для collection group `members` по полю `uid`, чтобы видеть пространства, где вы участник."
            } else {
                null
            }
        }

        val memberSpaces = memberSpacesResult.getOrElse { emptyList() }

        val spaces = (ownedSpaces + memberSpaces)
            .groupBy { it.space.id }
            .mapNotNull { (_, items) ->
                items.maxByOrNull { if (it.isOwner) 1 else 0 }
            }
            .sortedByDescending { it.space.updatedAt?.seconds ?: 0L }

        LoadSpacesResult(
            spaces = spaces,
            warningMessage = warningMessage
        )
    }

    suspend fun createSpace(
        name: String,
        description: String,
        owner: FirebaseUser
    ): String {
        val now = Timestamp.now()
        val spaceRef = firestore.collection("spaces").document()
        val memberRef = spaceRef.collection("members").document(owner.uid)
        val roleRefs = RoleTemplates.defaultRoles(now).associate { (roleId, _) ->
            roleId to spaceRef.collection("roles").document(roleId)
        }

        val space = Space(
            id = spaceRef.id,
            name = name,
            description = description,
            ownerId = owner.uid,
            createdAt = now,
            updatedAt = now,
            settings = SpaceSettings(defaultRoleId = RoleTemplates.VIEWER_ROLE_ID),
            stats = SpaceStats(members = 1, nodes = 0)
        )

        val ownerMember = SpaceMember(
            uid = owner.uid,
            email = owner.email.orEmpty(),
            displayName = owner.displayName.orEmpty(),
            roleId = "owner",
            joinedAt = now,
            invitedBy = owner.uid,
            lastActive = now
        )

        firestore.runBatch { batch ->
            batch.set(spaceRef, space)
            batch.set(memberRef, ownerMember)
            RoleTemplates.defaultRoles(now).forEach { (roleId, role) ->
                batch.set(roleRefs.getValue(roleId), role)
            }
        }.await()

        return spaceRef.id
    }

    suspend fun loadRolesSetup(spaceId: String): RolesSetupData {
        val spaceRef = firestore.collection("spaces").document(spaceId)
        val spaceSnapshot = spaceRef.get().await()
        val space = spaceSnapshot.toObject(Space::class.java)?.copy(id = spaceSnapshot.id)
            ?: throw IllegalStateException("Пространство не найдено")

        var roles = loadRoles(spaceId)
        if (roles.isEmpty()) {
            seedDefaultRoles(spaceRef)
            roles = loadRoles(spaceId)
        }

        val currentDefaultRoleId = space.settings.defaultRoleId ?: RoleTemplates.VIEWER_ROLE_ID
        if (space.settings.defaultRoleId == null) {
            updateDefaultRole(spaceId, currentDefaultRoleId)
        }

        return RolesSetupData(
            spaceId = space.id,
            spaceName = space.name,
            defaultRoleId = currentDefaultRoleId,
            roles = roles
        )
    }

    suspend fun createRole(
        spaceId: String,
        name: String,
        color: String
    ) {
        val roleRef = firestore.collection("spaces")
            .document(spaceId)
            .collection("roles")
            .document()

        roleRef.set(
            SpaceRole(
                id = roleRef.id,
                name = name,
                color = color,
                createdAt = Timestamp.now(),
                isSystem = false
            )
        ).await()
    }

    suspend fun updateRole(
        spaceId: String,
        roleId: String,
        name: String,
        color: String
    ) {
        val roleRef = firestore.collection("spaces")
            .document(spaceId)
            .collection("roles")
            .document(roleId)

        roleRef.update(
            mapOf(
                "name" to name,
                "color" to color
            )
        ).await()
    }

    suspend fun deleteRole(spaceId: String, roleId: String) {
        if (roleId == RoleTemplates.OWNER_ROLE_ID) {
            throw IllegalArgumentException("Роль Owner нельзя удалить")
        }

        val spaceRef = firestore.collection("spaces").document(spaceId)
        val space = spaceRef.get().await().toObject(Space::class.java)
            ?: throw IllegalStateException("Пространство не найдено")

        if (space.settings.defaultRoleId == roleId) {
            throw IllegalArgumentException("Сначала назначьте другую роль по умолчанию")
        }

        spaceRef.collection("roles").document(roleId).delete().await()
    }

    suspend fun updateDefaultRole(spaceId: String, roleId: String) {
        firestore.collection("spaces")
            .document(spaceId)
            .update("settings.defaultRoleId", roleId)
            .await()
    }

    private suspend fun loadRoles(spaceId: String): List<SpaceRole> {
        return firestore.collection("spaces")
            .document(spaceId)
            .collection("roles")
            .get()
            .await()
            .documents
            .mapNotNull { document ->
                document.toObject(SpaceRole::class.java)?.copy(id = document.id)
            }
            .sortedWith(compareByDescending<SpaceRole> { it.isSystem }.thenBy { it.name })
    }

    private suspend fun seedDefaultRoles(spaceRef: com.google.firebase.firestore.DocumentReference) {
        val now = Timestamp.now()
        firestore.runBatch { batch ->
            RoleTemplates.defaultRoles(now).forEach { (roleId, role) ->
                batch.set(spaceRef.collection("roles").document(roleId), role)
            }
            batch.update(spaceRef, "settings.defaultRoleId", RoleTemplates.VIEWER_ROLE_ID)
        }.await()
    }
}
