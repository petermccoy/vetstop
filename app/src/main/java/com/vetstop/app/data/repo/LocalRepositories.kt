package com.vetstop.app.data.repo

import com.vetstop.app.data.db.LocationDao
import com.vetstop.app.data.db.LocationWithLastVisit
import com.vetstop.app.data.db.PendingVisitDao
import com.vetstop.app.data.db.PendingVisitEntity
import com.vetstop.app.data.db.PendingVisitWithLocation
import com.vetstop.app.data.db.RecentVisit
import com.vetstop.app.data.db.SearchAreaDao
import com.vetstop.app.data.db.SearchAreaEntity
import com.vetstop.app.data.db.VisitDao
import com.vetstop.app.data.db.VisitEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class SearchAreaRepository @Inject constructor(
    private val dao: SearchAreaDao,
) {
    fun observeAll(): Flow<List<SearchAreaEntity>> = dao.observeAll()
    suspend fun getById(id: Long): SearchAreaEntity? = dao.getById(id)
    suspend fun add(area: SearchAreaEntity): Long = dao.insert(area)
    suspend fun update(area: SearchAreaEntity) = dao.update(area)
    suspend fun delete(area: SearchAreaEntity) = dao.delete(area)
}

@Singleton
class LocationRepository @Inject constructor(
    private val dao: LocationDao,
) {
    fun observeActiveWithLastVisit(): Flow<List<LocationWithLastVisit>> =
        dao.observeActiveWithLastVisit()

    fun observeByIdWithLastVisit(placeId: String): Flow<LocationWithLastVisit?> =
        dao.observeByIdWithLastVisit(placeId)

    fun observeActiveCount(): Flow<Int> = dao.observeActiveCount()
}

@Singleton
class VisitRepository @Inject constructor(
    private val dao: VisitDao,
    private val pendingDao: PendingVisitDao,
) {
    fun observeForPlace(placeId: String): Flow<List<VisitEntity>> = dao.observeForPlace(placeId)
    fun observeRecentWithLocation(limit: Int = 100): Flow<List<RecentVisit>> =
        dao.observeRecentWithLocation(limit)

    suspend fun getById(id: Long): VisitEntity? = dao.getById(id)
    suspend fun add(visit: VisitEntity): Long = dao.insert(visit)
    suspend fun update(visit: VisitEntity) = dao.update(visit)
    suspend fun delete(visit: VisitEntity) = dao.delete(visit)

    // --- Pending (not-yet-logged) trip stops ---

    fun observePending(): Flow<List<PendingVisitWithLocation>> =
        pendingDao.observeAllWithLocation()

    /** Queues the given places as stops awaiting a logged visit. */
    suspend fun addPending(placeIds: Collection<String>) {
        val now = System.currentTimeMillis()
        for (placeId in placeIds) {
            pendingDao.upsert(PendingVisitEntity(placeId = placeId, createdAt = now))
        }
    }

    suspend fun clearPending(placeId: String) = pendingDao.deleteByPlaceId(placeId)
}
