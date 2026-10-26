import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ToastModule } from 'primeng/toast';
import { Header } from '../header/header';

/** App layout: top bar, the current page, the toast for messages and the shared confirm dialog. */
@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, ToastModule, ConfirmDialogModule, Header],
  template: `
    <app-header />
    <main class="content">
      <router-outlet />
    </main>
    <p-toast position="top-right" />
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
