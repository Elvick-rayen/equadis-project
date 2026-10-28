/**
 * The API exchanges calendar dates as 'yyyy-MM-dd' (no time, no zone). These helpers convert
 * to and from Date objects at local midnight, so no timezone shift can change the day.
 */
export function toIsoDate(date: Date): string {
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${date.getFullYear()}-${month}-${day}`;
}

export function fromIsoDate(isoDate: string): Date {
  const [year, month, day] = isoDate.split('-').map(Number);
  return new Date(year, month - 1, day);
}

/** Calendar dates follow Europe/Paris (as the backend does), whatever the browser's time zone. */
const PARIS_DATE = new Intl.DateTimeFormat('en-CA', { timeZone: 'Europe/Paris' });

/** Today in Europe/Paris, so form defaults and minimum dates match what the API accepts. */
export function today(): Date {
  return fromIsoDate(PARIS_DATE.format(new Date()));
}

export function addDays(date: Date, days: number): Date {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate() + days);
}

export function addMonths(date: Date, months: number): Date {
  return new Date(date.getFullYear(), date.getMonth() + months, date.getDate());
}

/** 'yyyy-MM-dd' → 'dd.MM.yyyy', the display format used across the app (tables use the same via DatePipe). */
export function toDisplayDate(isoDate: string): string {
  const [year, month, day] = isoDate.split('-');
  return `${day}.${month}.${year}`;
}
