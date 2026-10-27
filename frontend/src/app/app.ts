import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ToastModule } from 'primeng/toast';

/** Root: the current page plus the toast, which every page (signed in or not) can use. */
@Component({
  selector: 'app-root',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, ToastModule],
  template: `
    <router-outlet />
    <p-toast position="top-right" />
  `,
})
export class App {}
