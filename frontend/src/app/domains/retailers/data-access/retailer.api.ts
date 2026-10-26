import { Injectable } from '@angular/core';
import { delay, Observable, of } from 'rxjs';
import { Retailer } from '../models/retailer.model';
import { RETAILER_SEED } from './retailer.seed';

/** Retailer API contract. Only listing exists so far (used by the "Acting as" selector). */
@Injectable({ providedIn: 'root', useFactory: () => new InMemoryRetailerApi() })
export abstract class RetailerApi {
  abstract list(): Observable<Retailer[]>;
}

const LATENCY_MS = 250;

@Injectable()
export class InMemoryRetailerApi extends RetailerApi {
  private readonly retailers: Retailer[] = structuredClone(RETAILER_SEED);

  list(): Observable<Retailer[]> {
    return of(structuredClone(this.retailers)).pipe(delay(LATENCY_MS));
  }
}
