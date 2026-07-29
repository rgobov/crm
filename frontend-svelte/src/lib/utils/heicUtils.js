/**
 * Конвертирует HEIC/HEIF файл в JPEG перед отправкой на сервер.
 * iPhone по умолчанию снимает в HEIC — бэкенд не поддерживает этот формат.
 * heic2any выполняет конвертацию на стороне браузера.
 *
 * @param {File} file - выбранный файл
 * @returns {Promise<File>} - JPEG файл (оригинальный, если не HEIC; сконвертированный, если HEIC)
 */
export async function ensureJpeg(file) {
    const heicTypes = ['image/heic', 'image/heif', 'image/heic-sequence', 'image/heif-sequence'];

    if (!heicTypes.includes(file.type)) {
        return file;
    }

    try {
        const { default: heic2any } = await import('heic2any');

        const jpegBlob = await heic2any({
            blob: file,
            toType: 'image/jpeg',
            quality: 0.9
        });

        const singleBlob = Array.isArray(jpegBlob) ? jpegBlob[0] : jpegBlob;

        const ext = file.name.includes('.') ? file.name.substring(0, file.name.lastIndexOf('.')) : file.name;
        return new File([singleBlob], ext + '.jpg', { type: 'image/jpeg' });
    } catch (e) {
        console.error('HEIC conversion failed, sending original:', e);
        return file;
    }
}
