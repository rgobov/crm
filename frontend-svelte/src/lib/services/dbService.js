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
            // Возвращаем весь объект { staffId, photoData, updatedAt, noPhoto }
            // noPhoto=true — признак что фото точно нет (кэш отрицательного результата)
        } catch (e) {
            console.error('DB Error getting photo:', e);
            return null;
        }
    },

    async savePhoto(staffId, photoData, updatedAt = Date.now()) {
        if (!staffId) return;
        try {
            // Разрешаем сохранять пустой photoData с признаком noPhoto —
            // чтобы кэшировать "фото точно нет" и не делать повторных API запросов
            const noPhoto = !photoData;
            await db.staffPhotos.put({ staffId, photoData: photoData || '', updatedAt, noPhoto });
        } catch (e) {
            console.error('DB Error saving photo:', e);
        }
    },

    async clearPhotos() {
        await db.staffPhotos.clear();
    }
};
