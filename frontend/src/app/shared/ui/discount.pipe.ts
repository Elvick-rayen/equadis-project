import { Pipe, PipeTransform } from '@angular/core';

export interface DiscountLike {
  discountType: 'PERCENTAGE_DISCOUNT' | 'FIXED_AMOUNT_DISCOUNT' | null;
  discountValue: number | null;
  currency: string | null;
}

/** `offer | discount: language()` → "15 %" or "250.00 EUR", formatted for the language. */
@Pipe({ name: 'discount' })
export class DiscountPipe implements PipeTransform {
  transform(offer: DiscountLike | null | undefined, language: string): string {
    if (!offer?.discountType || offer.discountValue === null) return '';
    if (offer.discountType === 'PERCENTAGE_DISCOUNT') {
      const value = new Intl.NumberFormat(language, { maximumFractionDigits: 2 }).format(offer.discountValue);
      return `${value} %`;
    }
    const amount = new Intl.NumberFormat(language, { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(
      offer.discountValue,
    );
    return `${amount} ${offer.currency ?? ''}`.trim();
  }
}
