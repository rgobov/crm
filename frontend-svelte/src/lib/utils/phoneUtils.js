const availableMasks = [
    { code: '7', mask: '+# (###) ###-##-##', country: 'RU/KZ' },
    { code: '375', mask: '+### (##) ###-##-##', country: 'BY' },
    { code: '380', mask: '+### (##) ###-##-##', country: 'UA' },
    { code: '998', mask: '+### (##) ###-##-##', country: 'UZ' },
    { code: '1', mask: '+# (###) ###-####', country: 'US/CA' }
].sort((a, b) => b.code.length - a.code.length);

export const phoneUtils = {
    clean(phone) {
        return phone.replace(/\D/g, '');
    },

    format(phone) {
        if (!phone) return '';
        const digits = this.clean(phone);
        if (!digits) return '';

        const maskObj = availableMasks.find(m => digits.startsWith(m.code));
        if (!maskObj) return '+' + digits;

        const mask = maskObj.mask;
        let result = '';
        let digitIdx = 0;

        for (let i = 0; i < mask.length && digitIdx < digits.length; i++) {
            if (mask[i] === '#') {
                result += digits[digitIdx];
                digitIdx++;
            } else {
                result += mask[i];
            }
        }

        if (digitIdx < digits.length) {
            result += digits.substring(digitIdx);
        }

        return result;
    }
};
