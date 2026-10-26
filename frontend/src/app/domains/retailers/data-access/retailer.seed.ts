import { Retailer } from '../models/retailer.model';

/** Demo data used by the in-memory API until the backend exists. */
export const RETAILER_SEED: Retailer[] = [
  {
    id: 'ret-city-market-lyon',
    storeName: 'City Market Lyon',
    storeType: 'PHYSICAL',
    location: { countryCode: 'FR', city: 'Lyon', address: '12 Rue de la République' },
    contactEmail: 'contact@citymarket-lyon.fr',
  },
  {
    id: 'ret-bio-co-paris',
    storeName: 'Bio&Co Paris',
    storeType: 'HYBRID',
    location: { countryCode: 'FR', city: 'Paris', address: '48 Boulevard Voltaire', website: 'https://bioco.fr' },
    contactEmail: 'hello@bioco.fr',
  },
  {
    id: 'ret-hyper-sud',
    storeName: 'Hyper Sud',
    storeType: 'PHYSICAL',
    location: { countryCode: 'FR', city: 'Marseille', address: '5 Avenue du Prado' },
    contactEmail: 'achats@hypersud.fr',
  },
  {
    id: 'ret-kiosk-express',
    storeName: 'Kiosk Express',
    storeType: 'ONLINE',
    location: { countryCode: 'BE', website: 'https://kioskexpress.be' },
    contactEmail: 'support@kioskexpress.be',
  },
];
