import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { Header } from '../header/header';

/** App layout for signed-in users: top bar, the current page and the shared confirm dialog. */
@Component({
  selector: 'app-shell',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, ConfirmDialogModule, Header],
  template: `
    <app-header />
    <main class="content">
      <router-outlet />
    </main>
    <p-confirmdialog [style]="{ width: '28rem' }" [breakpoints]="{ '640px': '95vw' }" />
  `,
  styles: `
    .content {
      max-width: 80rem;
      margin: 0 auto;
      padding: 1.5rem 1rem;
    }
  `,
})
export class Shell {}
