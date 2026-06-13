package com.example.data

import kotlinx.coroutines.flow.Flow

class RecordedClipRepository(private val clipDao: RecordedClipDao) {
    val allClips: Flow<List<RecordedClip>> = clipDao.getAllClips()

    suspend fun insert(clip: RecordedClip) {
        clipDao.insertClip(clip)
    }

    suspend fun delete(clip: RecordedClip) {
        clipDao.deleteClip(clip)
    }

    suspend fun deleteById(id: Int) {
        clipDao.deleteClipById(id)
    }

    suspend fun clearAll() {
        clipDao.clearAllClips()
    }
}
