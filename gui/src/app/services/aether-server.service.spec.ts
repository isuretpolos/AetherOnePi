import { TestBed } from '@angular/core/testing';

import { AetherServerService } from './aether-server.service';

describe('AetherServerService', () => {
  beforeEach(() => TestBed.configureTestingModule({}));

  it('should be created', () => {
    const service: AetherServerService = TestBed.get(AetherServerService);
    expect(service).toBeTruthy();
  });
});
