import { TestBed } from '@angular/core/testing';

import { TreatmentReportService } from './treatment-report.service';

describe('TreatmentReportService', () => {
  let service: TreatmentReportService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(TreatmentReportService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
