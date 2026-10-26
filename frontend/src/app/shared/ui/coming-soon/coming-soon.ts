import { Component, input } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';

/** Placeholder for screens that are not built yet. `titleKey` comes from the route data. */
@Component({
  selector: 'app-coming-soon',
  imports: [TranslocoPipe],
  template: `
    <h2>{{ titleKey() | transloco }}</h2>
    <p>{{ 'common.comingSoon' | transloco }}</p>
  `,
})
export class ComingSoon {
  readonly titleKey = input.required<string>();
}
