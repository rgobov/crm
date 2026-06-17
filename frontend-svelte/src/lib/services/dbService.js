import Dexie from 'dexie';

export const db = new Dexie('NeuroCRM_DB');

db.version(1).stores({
    staffPhotos: 'staffId', // staffId - первичный ключ
    // В будущем сюда можно добавить кэш расписания:
    // appointments: 'id, date, branchId'
});

export const dbService = {
    async getPhoto(staffId) {
        if (!staffId) return null;
        try {
            return await db.staffPhotos.get(staffId);
            // Возвращаем весь объект { staffId, photoData, updatedAt }
        } catch (e) {
            console.error('DB Error getting photo:', e);
            return null;
        }
    },

    async savePhoto(staffId, photoData, updatedAt = Date.now()) {
        if (!staffId || !photoData) return;
        try {
            await db.staffPhotos.put({ staffId, photoData, updatedAt });
        } catch (e) {
            console.error('DB Error saving photo:', e);
        }
    },

    async clearPhotos() {
        await db.staffPhotos.clear();
    }
};
