export const NICHES = {
    AUTO: 'AUTO',
    BEAUTY: 'BEAUTY',
    RENT: 'RENT'
};

export const NICHE_LIST = [
    { value: 'AUTO',   label: 'Автосервис', icon: '🚗' },
    { value: 'BEAUTY', label: 'Бьюти',     icon: '💅' },
    { value: 'RENT',   label: 'Аренда',    icon: '🏠' }
];

export const NICHE_CONFIG = {
    AUTO: {
        assetLabel: 'Связанные объекты',
        assetIcon: '🚗',
        assetPlaceholder: 'Марка, госномер, S/N...',
        assetAddBtn: '+ Объект',
        assetAriaLabel: 'Удалить объект',
        refLabel: 'АВТОМОБИЛЬ / ОБЪЕКТ',
        refPlaceholder: 'Марка, модель, госномер...',
        refIcon: '🚗',
        refDetailLabel: 'Объект визита'
    },
    BEAUTY: {
        assetLabel: 'Услуги и процедуры',
        assetIcon: '💅',
        assetPlaceholder: 'Окрашивание, стрижка...',
        assetAddBtn: '+ Заметку',
        assetAriaLabel: 'Удалить заметку',
        refLabel: 'ЗАМЕТКА К ВИЗИТУ',
        refPlaceholder: 'Пожелания, аллергии, длина...',
        refIcon: '📝',
        refDetailLabel: 'Заметка к визиту'
    },
    RENT: {
        assetLabel: 'Арендуемые объекты',
        assetIcon: '🏠',
        assetPlaceholder: 'Номер объекта, описание...',
        assetAddBtn: '+ Объект',
        assetAriaLabel: 'Удалить объект',
        refLabel: 'ОБЪЕКТ АРЕНДЫ',
        refPlaceholder: 'Номер квартиры, авто госномер...',
        refIcon: '🏠',
        refDetailLabel: 'Объект аренды'
    }
};

export function getNicheConfig(niche) {
    return NICHE_CONFIG[niche] || NICHE_CONFIG.AUTO;
}

export function getNicheIcon(niche) {
    const found = NICHE_LIST.find(n => n.value === niche);
    return found ? found.icon : '🏢';
}