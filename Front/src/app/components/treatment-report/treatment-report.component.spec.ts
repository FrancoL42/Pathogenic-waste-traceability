import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TreatmentReportComponent } from './treatment-report.component';

describe('TreatmentReportComponent', () => {
  let component: TreatmentReportComponent;
  let fixture: ComponentFixture<TreatmentReportComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TreatmentReportComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TreatmentReportComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
