import { PersonaRole } from '../../shared/persona/persona.model';

export interface NavItem {
  /** Key under `nav.` in the translation files. */
  labelKey: string;
  route: string;
  icon: string;
}

/** Top bar menu for each persona. */
export const PERSONA_MENU: Record<PersonaRole, NavItem[]> = {
  ADMIN: [
    { labelKey: 'suppliers', route: '/suppliers', icon: 'pi pi-building' },
    { labelKey: 'retailers', route: '/retailers', icon: 'pi pi-shop' },
    { labelKey: 'partnerships', route: '/partnerships', icon: 'pi pi-link' },
  ],
  SUPPLIER: [{ labelKey: 'myOffers', route: '/my-offers', icon: 'pi pi-tags' }],
  RETAILER: [
    { labelKey: 'availableOffers', route: '/offers', icon: 'pi pi-tag' },
    { labelKey: 'mySubscriptions', route: '/subscriptions', icon: 'pi pi-bookmark' },
  ],
};
