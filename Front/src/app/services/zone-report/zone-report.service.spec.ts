import { TestBed } from '@angular/core/testing';

import { ZoneReportService } from './zone-report.service';

describe('ZoneReportService', () => {
  let service: ZoneReportService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ZoneReportService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
