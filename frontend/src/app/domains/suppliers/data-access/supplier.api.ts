import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE, Page, PageRequest } from '../../../shared/http/api';
import { Supplier, SupplierDraft, SupplierSortField, SupplierSummary } from '../models/supplier.model';

export interface SupplierSearch extends PageRequest<SupplierSortField> {
  query?: string;
}

/** HTTP client for /api/suppliers. Errors arrive as ApiError (see apiErrorInterceptor). */
@Injectable({ providedIn: 'root' })
export class SupplierApi {
  private readonly http = inject(HttpClient);
  private readonly url = `${API_BASE}/suppliers`;

  search(search: SupplierSearch): Observable<Page<Supplier>> {
    let params = new HttpParams().set('page', search.page).set('size', search.size);
    if (search.query?.trim()) {
      params = params.set('q', search.query.trim());
    }
    if (search.sort) {
      params = params.set('sort', `${search.sort.field},${search.sort.direction}`);
    }
    return this.http.get<Page<Supplier>>(this.url, { params });
  }

  summaries(): Observable<SupplierSummary[]> {
    return this.http.get<SupplierSummary[]>(`${this.url}/summaries`);
  }

  create(draft: SupplierDraft): Observable<Supplier> {
    return this.http.post<Supplier>(this.url, draft);
  }

  update(id: number, draft: SupplierDraft): Observable<Supplier> {
    return this.http.put<Supplier>(`${this.url}/${id}`, draft);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}
