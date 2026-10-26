import { Pipe, PipeTransform } from '@angular/core';
import { countryName } from './countries';

/** `'FR' | countryName: language()` → "France" / "France". Pass the language so it re-renders on change. */
@Pipe({ name: 'countryName' })
export class CountryNamePipe implements PipeTransform {
  transform(code: string, language: string): string {
    return countryName(code, language);
  }
}
