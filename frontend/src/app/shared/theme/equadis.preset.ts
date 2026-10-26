import { definePreset } from '@primeuix/themes';
import Aura from '@primeuix/themes/aura';

const NAVY = '#081E32';
const NAVY_HOVER = '#12324F';
const WHITE = '#FFFFFF';

/** Aura preset with the Equadis colours: green primary, navy top bar and table headers. */
export const EquadisPreset = definePreset(Aura, {
  semantic: {
    primary: {
      50: '#effdf5',
      100: '#d8fbe7',
      200: '#b3f6d0',
      300: '#88f1b8',
      400: '#6eeeaa',
      500: '#58ea9d',
      600: '#2fd27e',
      700: '#22a764',
      800: '#1d8150',
      900: '#186241',
      950: '#0b3a25',
    },
    colorScheme: {
      light: {
        primary: {
          color: '{primary.500}',
          // Navy text on green keeps buttons readable.
          contrastColor: NAVY,
          hoverColor: '{primary.600}',
          activeColor: '{primary.700}',
        },
      },
    },
  },
  components: {
    menubar: {
      root: {
        background: NAVY,
        borderColor: NAVY,
        color: WHITE,
        borderRadius: '0',
      },
      item: {
        color: WHITE,
        focusColor: WHITE,
        activeColor: NAVY,
        focusBackground: NAVY_HOVER,
        activeBackground: '{primary.500}',
        icon: {
          color: WHITE,
          focusColor: WHITE,
          activeColor: NAVY,
        },
      },
      submenu: {
        background: NAVY,
        borderColor: NAVY_HOVER,
      },
      mobileButton: {
        color: WHITE,
        hoverColor: WHITE,
        hoverBackground: NAVY_HOVER,
      },
    },
    datatable: {
      headerCell: {
        background: NAVY,
        color: WHITE,
        borderColor: NAVY,
        hoverBackground: NAVY_HOVER,
        hoverColor: WHITE,
        selectedBackground: NAVY,
        selectedColor: '{primary.500}',
      },
      columnTitle: {
        fontWeight: '600',
      },
      sortIcon: {
        color: WHITE,
        hoverColor: WHITE,
      },
    },
  },
});
