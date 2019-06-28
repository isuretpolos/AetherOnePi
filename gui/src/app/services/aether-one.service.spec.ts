import { TestBed } from '@angular/core/testing';

import { AetherOneService } from './aether-one.service';

describe('AetherOneService', () => {
  beforeEach(() => TestBed.configureTestingModule({}));

  it('should be created', () => {
    const service: AetherOneService = TestBed.get(AetherOneService);
    expect(service).toBeTruthy();
  });
});
